(ns rouje-like.lichen
  (:require [rouje-like.entity :as rj.e]))

(defn process-input-tick!
  [system this _]
  system)

(defn take-damage!
  [system this damage]
  (let [c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)#_(ATOM)

        c-position (rj.e/get-c-on-e system this :position)
        world (:world c-position)#_(ATOM)]
    (if (pos? (- @hp damage))
      (swap! hp - damage)
      (swap! world (fn [world]
                     (update-in world [@(:x c-position) @(:y c-position)]
                                (fn [tile]
                                  (update-in tile [:entities]
                                             (fn [entities]
                                               (vec (remove #(#{:lichen} (:type %))
                                                            entities)))))))))))
