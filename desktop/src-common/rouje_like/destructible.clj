(ns rouje-like.destructible
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :refer [can-attack? attack]]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.messaging :as rj.msg]
            [clojure.set :refer [intersection difference]]))

(defn add-effects
  [system e-this e-from]
  "Adds applicable status effects from e-from to e-this."
  (let  [c-attacker (rj.e/get-c-on-e system e-from :attacker)
         attacker-effects (:status-effects c-attacker)
         c-destructible (rj.e/get-c-on-e system e-this :destructible)
         status (:status-effects c-destructible)
         status-intersection (fn [my-status-effects incoming-status-effects]
                               (filter #((apply hash-set (map (partial :type) incoming-status-effects))
                                         (:type %))
                                       my-status-effects))
         status-difference (fn [my-status-effects incoming-status-effects]
                             (filter #(not ((apply hash-set (map (partial :type) incoming-status-effects))
                                            (:type %)))
                                     my-status-effects))
         status->value (fn [status]
                         (* (:value status)
                            (:duration status)))]
    (as->
      ;; Add/keep the better status effect
      (if-let [intersection (seq (vec (status-intersection attacker-effects status)))]
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
            system) system
          ;; Add status effects that e-this does not have
          (if-let [diff (seq (vec (status-difference attacker-effects status)))]
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
  (not (nil? system))
  (let [c-destructible (rj.e/get-c-on-e system e-this :destructible)
        statuses (:status-effects c-destructible)
        dec-status-effects (fn [status-effects]
                             (vec (filter identity
                                          (map (fn [status]
                                                 (let [status- (update-in status [:duration]
                                                                          dec)]
                                                   (if (neg? (:duration status-))
                                                     nil
                                                     status-)))
                                               status-effects))))]
    (reduce (fn [system status]
              (as-> (rj.e/upd-c system e-this :destructible
                                (fn [c-destructible]
                                  (update-in c-destructible [:status-effects]
                                             dec-status-effects))) system
                (if-let [status (seq (filter #(#{(:type status)} (:type %))
                                             (:status-effects (rj.e/get-c-on-e system e-this :destructible))))]
                  ((:apply-fn (first status)) system e-this (first status))
                  system)))
            system statuses)))

(defn take-damage
  [c-this e-this damage e-from system]
  (let [hp (:hp c-this)

        def (:def (rj.e/get-c-on-e system e-this :destructible))
        damage (if (pos? (- damage def))
                 (rj.u/rand-rng 1 (- damage def))
                 (if (zero? damage)
                   0 1))

        c-position (rj.e/get-c-on-e system e-this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (as-> system system
        (rj.e/upd-c system e-this :destructible
                    (fn [c-destructible]
                      (update-in c-destructible [:hp] - damage)))
        (add-effects system e-this e-from)

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
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  damage ((:name-fn c-broadcaster) system e-this)))
          system))

      (as-> system system
        (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
          (rj.msg/add-msg system :static
                          (format "%s killed %s"
                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                    ((:name-fn atker-c-broadcaster) system e-from))
                                  ((:name-fn c-broadcaster) system e-this)))
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

        (if-let [on-death (:on-death-fn c-this)]
          (on-death c-this e-this system)
          system)

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
