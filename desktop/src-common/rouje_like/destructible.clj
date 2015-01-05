(ns rouje-like.destructible
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :refer [can-attack? attack
                                           ->3DPoint]]
            [rouje-like.utils :as rj.u :refer [? as-?>]]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.messaging :as rj.msg]
            [clojure.set :refer [intersection difference]]))

(defn merge-by-with
  "Merge COLL1 and COLL2 using BY to process each element
   and WITH to choose between two conflicting elements.
   See t_destructible.clj for examples."
  [coll1 coll2 by with]
  (reduce (fn [acc a]
            (if-let [b (seq (filter #(= (by a)
                                        (by %))
                                    acc))]
              (conj (remove (set b) acc)
                    (reduce #(with %1 %2)
                            a b))
              (conj acc a)))
          coll1 coll2))

(defn pick-by-with
  "Pick between I and J using WITH
   to compare between (BY I) and (BY J).
   Note: to pick I, WITH has to return true
         otherwise it picks J.
   See t_destructible.clj for examples."
  [by with]
  (fn [x y]
    (if (with
          (by x)
          (by y))
      x y)))

(defn pick-by-with-
  "Same as pick-by-with, except WITH must
   return the BY'ed element it prefers,
   or it will throw an exception,
   or return a default value.
   See t_destructible.clj for examples."
  ([by with]
   (pick-by-with- by with nil))
  ([by with dflt]
   (fn [x y]
     (let [by-x (by x)
           by-y (by y)
           r (with by-x by-y)]
       (cond
         (= r by-x) x

         (= r by-y) y

         :else dflt)))))

(defn add-effects
  [system e-this e-from]
  "Adds/merges applicable status effects from e-from to e-this."
  (let [c-attacker (rj.e/get-c-on-e system e-from :attacker)
        attacker-effects (:status-effects c-attacker)
        status->value (fn [{:keys [value duration]}]
                        (* value duration))]
    (rj.stef/update-in-status-effects
      system e-this (fn [status-effects]
                      (merge-by-with
                        attacker-effects status-effects
                        :type (pick-by-with
                                status->value >))))))

(defn apply-effects
  [system e-this]
  "Applies status effects from e-this's destructible status-effects to e-this"
  (let [dec-status-effects (fn [status-effects]
                             (vec
                               (->> status-effects
                                    (map #(update-in % [:duration] dec))
                                    (filter (comp #(not (neg? %))
                                                  :duration)))))]
    (as-> system system
      ;;decrease the duration (by 1) of all status-effects
      ;;if it goes negative, remove it
      (rj.e/upd-c system e-this :destructible
                  (fn [c-destructible]
                    (update-in c-destructible [:status-effects]
                               dec-status-effects)))
      ;;apply each remaining status effect to the e-this
      (reduce (fn [system status]
                ((:apply-fn status) system e-this status))
              system (:status-effects (rj.e/get-c-on-e system e-this :destructible))))))

(defn take-damage
  [c-this e-this damage e-from system]
  (let [{:keys [hp def]} c-this

        damage (if (zero? damage)
                 0
                 (max 1 (rj.u/rand-rng 1 (- damage def))))

        c-position (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      ;;e-this is still alive
      (as-?> system
        ;;update its hp, ie: take the damage
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp]
                                 - damage)))

        ;;recieve any status-effects from e-from
        (add-effects system e-this e-from)

        ;;if it can retaliate & attack, strike back!
        (if-let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
          (if (and (:can-retaliate? c-this)
                   (can-attack? c-attacker e-this e-from system))
            (attack c-attacker e-this e-from system)))

        ;;notify of damage taken
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system
                          (format "%s dealt %s damage to %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  damage ((:name-fn c-broadcaster) system e-this)))))

      ;;e-this has died
      (as-?> system
        ;;if it can retaliate, strike back!
        (if-let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
          (if (and (:can-retaliate? c-this)
                   (can-attack? c-attacker e-this e-from system))
            (attack c-attacker e-this e-from system)))

        ;;call the on-death-fn, eg: amoeba splitting
        (if-let [on-death (:on-death-fn c-this)]
          (on-death c-this e-this system))

        ;;give the attacker exp & maybe lvl it up
        (if-let [c-killable (rj.e/get-c-on-e system e-this :killable)]
          (let [c-exp (rj.e/get-c-on-e system e-from :experience)
                level-up-fn (:level-up-fn c-exp)]

            (->> (rj.e/upd-c system e-from :experience
                             (fn [c-experience]
                               (update-in c-experience [:experience]
                                          #(+ % (:experience c-killable)))))
                 (level-up-fn e-from))))

        ;;notify of death/kill
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system
                          (format "%s killed %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  ((:name-fn c-broadcaster) system e-this))))

        ;;remove it from the world
        (rj.u/update-in-world system e-world
                              (->3DPoint c-position)
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities))))

        ;;finally remove e-this from system
        (rj.e/kill-e system e-this)))))
