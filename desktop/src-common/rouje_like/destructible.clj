(ns rouje-like.destructible
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :refer [can-attack? attack]]
            [rouje-like.utils :as rj.u]
            [rouje-like.messaging :as rj.msg]
            [clojure.set :refer [intersection difference]]))

(defn take-damage
  [c-this e-this damage e-from system]
  (let [hp (:hp c-this)

        def (:def (rj.e/get-c-on-e system e-this :destructible))
        damage (rj.u/rand-rng 1 (- damage def)) ;rand[1,(dmg-def)]

        c-position (rj.e/get-c-on-e system e-this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (as-> system system
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp] - damage)))

        (if-let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
          (if (and (:can-retaliate? c-this)
                   (can-attack? c-attacker e-this e-from system))
            (attack c-attacker e-this e-from system)
            system)
          system)

        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s dealt %s damage to %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:msg-fn atker-c-broadcaster) system e-from))
                                  damage ((:msg-fn c-broadcaster) system e-this)))
          system))

      (as-> system system
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s killed %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:msg-fn atker-c-broadcaster) system e-from))
                                  ((:msg-fn c-broadcaster) system e-this)))
          system)

        (if-let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
          (if (and (:can-retaliate? c-this)
                   (can-attack? c-attacker e-this e-from system))
            (attack c-attacker e-this e-from system)
            system)
          system)

        (rj.u/update-in-world system e-world [(:z c-position) (:x c-position) (:y c-position)]
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities))))

        (if-let [c-killable (rj.e/get-c-on-e system e-this :killable)]
          (let [c-exp (rj.e/get-c-on-e system e-from :experience)
                level-up-fn (:level-up-fn c-exp)]

            (->> (rj.e/upd-c system e-from :experience
                             (fn [c-experience]
                               (update-in c-experience [:experience]
                                          #(+ % (:experience c-killable)))))
                 (level-up-fn e-from)))
          system)
        (rj.e/kill-e system e-this)))))

(defn add-effect
  [system e-this e-from]
  (let  [c-attacker (rj.e/get-c-on-e system e-from :attacker)
         attacker-effects (:status-effects c-attacker) #_(Effect vector that e-from has)
         c-destructible (rj.e/get-c-on-e system e-this :destructible)
         status (:status-effects c-destructible)]  #_(Current status(es) of e-this)

    (as-> (if-let [intersect (seq (vec (intersection (apply hash-set status) (apply hash-set attacker-effects))))] #_(If already in status)
            ((rj.e/upd-c system e-this c-destructible  #_(Refresh duplicate status effects)
                         (fn [c-destructible] #_(How do I refresh all status effects in intersect?)
                           (update-in c-destructible [:status-effects] (partial (conj (c-destructible :status-effects) 4))))))
            system) system
          (if-let [diff (seq (vec (difference (apply hash-set attacker-effects) (apply hash-set status))))] #_(If not in status)
            system #_(Add effects to status vector. How do I add status effects all in diff? Pull maps from diff and conj?)
            system))))

(defn remove-effect
  [system e-this e-from]
  system
  )

(defn apply-effect
  [system e-this e-from]
  system
  )
