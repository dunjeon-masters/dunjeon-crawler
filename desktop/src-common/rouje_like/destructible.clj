(ns rouje-like.destructible
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :refer [can-attack? attack]]))

(defn take-damage
  [c-this e-this damage e-from system]
  (let [hp (:hp c-this)

        c-position (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c e-this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage)))
          (as-> system
                (let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
                  (if (and (:can-retaliate? c-this)
                           (not (nil? c-attacker))
                           (can-attack? c-attacker e-this e-from system))
                    (attack c-attacker e-this e-from system)
                    system))))
      (-> system
          (as-> system
                (let [c-attacker (rj.e/get-c-on-e system e-this :attacker)]
                  (if (and (:can-retaliate? c-this)
                           (not (nil? c-attacker))
                           (can-attack? c-attacker e-this e-from system))
                    (attack c-attacker e-this e-from system)
                    system)))

          (rj.wr/update-in-world e-world [(:x c-position) (:y c-position)]
                                 (fn [entities]
                                   (vec
                                     (remove
                                       #(#{e-this} (:id %))
                                       entities))))
          (as-> system (if-let [c-killable (rj.e/get-c-on-e system e-this :killable)]
                         (rj.e/upd-c system e-from :experience
                                     (fn [c-experience]
                                       (update-in c-experience [:experience]
                                                  #(+ % (:experience (rj.e/get-c-on-e system e-this :killable))))))
                         system))
          (rj.e/kill-e e-this)))))
