(ns rouje-like.destructible
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :refer [can-attack? attack]]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.messaging :as rj.msg]
            [clojure.set :refer [intersection difference]]))

(defn add-effects
  [system e-this e-from]
  "Adds/merges applicable status effects from e-from to e-this."
  (let [c-attacker (rj.e/get-c-on-e system e-from :attacker)
        attacker-effects (:status-effects c-attacker)
        {:keys [status-effects]} (rj.e/get-c-on-e system e-this :destructible)
        status-intersection (fn [my-status-effects incoming-status-effects]
                              (filter #((apply hash-set
                                               (map :type incoming-status-effects))
                                        (:type %))
                                      my-status-effects))
        status-difference (fn [my-status-effects incoming-status-effects]
                            (filter #(not ((apply hash-set
                                                  (map :type incoming-status-effects))
                                           (:type %)))
                                    my-status-effects))
        status->value (fn [{:keys [value duration]}]
                        (* value duration))]
    (as-> system system
      ;; Add/keep the better status effect
      (if-let [intersection (seq (vec (status-intersection attacker-effects status-effects)))]
        (reduce (fn [system status]
                  (rj.e/upd-c system e-this :destructible
                              (fn [c-destructible]
                                (update-in c-destructible [:status-effects]
                                           (fn [status-effects]
                                             (vec (map (fn [my-status]
                                                         (if (not= (:type my-status) (:type status))
                                                           my-status
                                                           (if (< (status->value my-status)
                                                                  (status->value status))
                                                             status
                                                             my-status)))
                                                       status-effects)))))))
                system intersection)
        system)
      ;; Add status effects that e-this does not have
      (if-let [diff (seq (vec (status-difference attacker-effects status-effects)))]
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:status-effects]
                                 (fn [status-effects]
                                   (vec (concat status-effects
                                                diff))))))
        system))))

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
        damage (if (pos? (- damage def))
                 (rj.u/rand-rng 1 (- damage def))
                 (if (zero? damage)
                   0 1))

        c-position (rj.e/get-c-on-e system e-this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      ;;e-this is still alive
      (as-> system system
        ;;update its hp, ie: take the damage
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp] - damage)))
        ;;recieve any status-effects from e-from
        (add-effects system e-this e-from)

        ;;if it can retaliate & attack, strike back!
        (if-let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
          (if (and (:can-retaliate? c-this)
                   (can-attack? c-attacker e-this e-from system))
            (attack c-attacker e-this e-from system)
            system)
          system)

        ;;notify of damage taken
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s dealt %s damage to %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  damage ((:name-fn c-broadcaster) system e-this)))
          system))

      ;;e-this has died
      (as-> system system
        ;;if it can retaliate, strike back!
        (if-let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
          (if (and (:can-retaliate? c-this)
                   (can-attack? c-attacker e-this e-from system))
            (attack c-attacker e-this e-from system)
            system)
          system)

        ;;call the on-death-fn, eg: amoeba splitting
        (if-let [on-death (:on-death-fn c-this)]
          (on-death c-this e-this system)
          system)

        ;;give the attacker exp & maybe lvl it up
        (if-let [c-killable (rj.e/get-c-on-e system e-this :killable)]
          (let [c-exp (rj.e/get-c-on-e system e-from :experience)
                level-up-fn (:level-up-fn c-exp)]

            (->> (rj.e/upd-c system e-from :experience
                             (fn [c-experience]
                               (update-in c-experience [:experience]
                                          #(+ % (:experience c-killable)))))
                 (level-up-fn e-from)))
          system)

        ;;notify of death/kill
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s killed %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  ((:name-fn c-broadcaster) system e-this)))
          system)

        ;;remove it from the world
        (rj.u/update-in-world system e-world
                              [(:z c-position) (:x c-position) (:y c-position)]
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities))))

        ;;finally remove e-this from system
        (rj.e/kill-e system e-this)))))
