(ns rouje-like.forest
  (:require [brute.entity :as br.e]

            [rouje-like.utils :as rj.u]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.config :as rj.cfg]))

(defn- block->freqs
  [block]
  (frequencies
    (map (fn [tile]
           (:type (rj.u/tile->top-entity tile)))
         block)))

(defn- get-smoothed-tile
  [block-d1 block-d2 x y z]
  (let [wall-threshold-d1 5
        wall-bound-d2 2
        top-entity (rj.u/tile->top-entity
                     (first (filter (fn [tile]
                                      (and (= x (:x tile)) (= y (:y tile))))
                                    block-d1)))
        this-id (:id top-entity)
        d1-block-freqs (block->freqs block-d1)
        d2-block-freqs (if (nil? block-d2)
                         {:tree (inc wall-bound-d2)}
                         (block->freqs block-d2))
        wall-count-d1 (get d1-block-freqs :tree 0)
        wall-count-d2 (get d2-block-freqs :tree 0)
        result (if (or (>= wall-count-d1 wall-threshold-d1)
                       (<= wall-count-d2 wall-bound-d2))
                 :tree
                 :forest-floor)]
    (update-in (rj.c/map->Tile {:x x :y y :z z
                                :entities [(rj.c/map->Entity {:id   nil
                                                              :type :forest-floor})]})
               [:entities] (fn [entities]
                             (if (= result :tree)
                               (conj entities
                                     (rj.c/map->Entity {:id   (if this-id
                                                                this-id
                                                                (br.e/create-entity))
                                                        :type :tree}))
                               entities)))))

(defn- get-smoothed-col
  [level [x z] max-dist]
  {:pre [(#{1 2} max-dist)]}
  (mapv (fn [y]
          (get-smoothed-tile
            (rj.u/get-ring-around level [x y] 1)
            (if (= max-dist 2)
              (rj.u/get-ring-around level [x y] 2)
              nil)
            x y z))
        (range (count (first level)))))

(defn- smooth-level-v1
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (get-smoothed-col level [x z] 2))
                (range (count level)))
   :z z})

(defn- smooth-level-v2
  [{:keys [level z]}]
  {:level (mapv (fn [x]
                  (get-smoothed-col level [x z] 1))
                (range (count level)))
   :z z})

(defn create-floor
  [x y z]
  (rj.c/map->Tile
    {:x x :y y :z z
     :entities [(rj.c/map->Entity {:id   nil
                                   :type :forest-floor})]}))

(defn ?add-tree
  [entities]
  (if (< (rand-int 100)
         (:wall rj.cfg/world-entity->spawn%))
    (conj entities
          (rj.c/map->Entity {:id   (br.e/create-entity)
                             :type :tree}))
    entities))

(defn generate-forest
  [[width height] z]
  (let [level (vec
                (map vec
                     (for [x (range width)]
                       (for [y (range height)]
                         (update-in (create-floor x y z)
                                    [:entities] ?add-tree)))))]
    ;; SMOOTH-WORLD
    (as-> level level
      (nth (iterate smooth-level-v1 {:level level
                                            :z z})
           2)
      (:level level)
      (nth (iterate smooth-level-v2 {:level level
                                            :z z})
           3)
      (:level level))))
