(ns rouje-like.destructible
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :refer [can-attack? attack]]))

(defn take-damage
  [c-this e-this damage e-from system]
  (let [hp (:hp c-this)

        c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)

        e-relay (first (rj.e/all-e-with-c system :relay))

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
                    system)))
          (as-> system
                (if (not (nil? c-broadcaster))
                  (rj.e/upd-c system e-relay :relay
                              (fn [c-relay]
                                (update-in c-relay [:static]
                                           conj {:message (format "%s dealt %s damage to %s"
                                                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                                                    ((:msg-fn atker-c-broadcaster) system e-from))
                                                                  damage ((:msg-fn c-broadcaster) system e-this))
                                                 :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                                             c-counter (rj.e/get-c-on-e system e-counter :counter)]
                                                         (:turn c-counter))})))
                  system)))
      (-> system
          (as-> system
                (if (not (nil? c-broadcaster))
                  (rj.e/upd-c system e-relay :relay
                              (fn [c-relay]
                                (update-in c-relay [:static]
                                           conj {:message (format "%s killed %s"
                                                                  (let [atker-c-broadcaster (rj.e/get-c-on-e system e-from :broadcaster)]
                                                                    ((:msg-fn atker-c-broadcaster) system e-from))
                                                                  ((:msg-fn c-broadcaster) system e-this))
                                                 :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                                             c-counter (rj.e/get-c-on-e system e-counter :counter)]
                                                         (:turn c-counter))})))
                  system))
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
          (rj.e/kill-e e-this)))))
