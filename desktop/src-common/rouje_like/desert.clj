(ns rouje-like.desert
  (:require [brute.entity :as br.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.config :as rj.cfg]
            [rouje-like.rooms :as rj.rm]))

(defn new-entity
  ([of-type]
   (new-entity of-type nil))
  ([of-type extra]
   (rj.c/map->Entity {:id (br.e/create-entity)
                      :type of-type
                      :extra extra})))

(defn generate-desert
  [[width height] z]
  (let [level (vec
                (map vec
                     (for [x (range width)]
                       (for [y (range height)]
                         (rj.c/map->Tile {:x x :y y :z z
                                          :entities [(rj.c/map->Entity {:id   nil
                                                                        :type :dune})]})))))
        desert (:level (rj.rm/print-level
                         (rj.rm/gen-level-with-rooms
                           width height (/ (* width height) 100) 5)))]
    (reduce (fn [level cell]
              (case (cell 2)
                :w (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (new-entity :temple-wall))))
                :f (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           (fn [entities]
                                             (remove #(#{:wall} (:type %))
                                                     entities)))))
                :st (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (new-entity :hidden-spike-trap
                                                            (cell 3)))))
                :at (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (new-entity :arrow-trap
                                                            (cell 3)))))
                :d (update-in level [(cell 0) (cell 1)]
                              (fn [tile]
                                (update-in tile [:entities]
                                           conj (new-entity :door))))
                level))
            level (map vec (partition 4 (flatten desert))))))
