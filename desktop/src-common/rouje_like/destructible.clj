(ns rouje-like.destructible
  (:require [rouje-like.entity :as rj.e]
            [rouje-like.world :as rj.wr]))

(defn take-damage
  [c-this e-this damage _ system]
  (let [hp (:hp c-this)

        c-position (rj.e/get-c-on-e system e-this :position)
        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c e-this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage))))
      (-> system
          (rj.wr/update-in-world e-world [(:x c-position) (:y c-position)]
                                 (fn [entities _]
                                   (vec
                                     (remove
                                       #(#{e-this} (:id %))
                                       entities))))
          (rj.e/kill-e e-this)))))
