(ns rouje-like.utils
  (:require [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer [pprint]]

            [rouje-like.components :as rj.c]))

(def get-default-pri
  {:floor 1
   :torch 2
   :gold 3
   :wall 4
   :lichen 5
   :bat 6
   :else 7
   :player 8})

(defn sort-by-pri
  [entities get-pri]
  (sort (fn [arg1 arg2]
          (let [t1 (:type arg1)
                t2 (:type arg2)]
            (if (= t1 t2)
              0
              (- (get get-pri t2
                      (get get-pri :else 1))
                 (get get-pri t1
                      (get get-pri :else 1))))))
        entities))

(defn get-top-entity
  ([target]
   (get-top-entity target get-default-pri))
  ([target get-pri]
   (-> target
       (:entities)
       (sort-by-pri get-pri)
       (first))))



(def ^:private directions
  {:left  [-1 0]
   :right [1  0]
   :up    [0  1]
   :down  [0 -1]})

(defn ^:private offset-coords
  "Offset the first coordinate by the second,
  returning the result coordinate."
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn ^:private get-neighbors-coords
  "Return the coordinates of all neighboring
  (ie: up/down/left/right)
  squares of the given [x y] pos."
  [origin]
  (map offset-coords
       (repeat origin) (vals directions)))

(defn get-neighbors
  [world origin]
  (map (fn [vec] (get-in world vec nil))
       (get-neighbors-coords origin)))

(defn get-neighbors-of-type
  [world origin type]
  (->> (get-neighbors world origin)
       (filter #(and (not (nil? %))
                     ((into #{} type) (:type (get-top-entity %)))))))

(defn ^:private radial-distance
  [[x1 y1] [x2 y2]]
  (max (math/abs (- x1 x2))
       (math/abs (- y1 y2))))

(defn get-entities-radially
  [world origin dist-fn]
  (filter #(dist-fn (radial-distance origin [(:x %) (:y %)]))
          (flatten world)))

(defn not-any-radially-of-type
  [world origin dist-fn type]
  (not-any? (fn [tile]
              ((into #{} type) (:type (get-top-entity tile
                                                      (zipmap (conj type :else)
                                                              (conj (vec
                                                                      (repeat (count type) 2))
                                                                    1))))))
            (get-entities-radially world origin dist-fn)))

(defn ^:private ring-coords
  [[x y] dist]
  (let [∆x|y (vec (range (- 0 dist) (inc dist)))]
    (for [dx ∆x|y
          dy ∆x|y
          :when (or (if (= dist 1)
                      true)
                    (= dist (math/abs dx))
                    (= dist (math/abs dy)))]
      [(+ x dx) (+ y dy)])))

(defn get-ring-around
  [tiles origin dist]
  (map (fn [[x y]]
         (get-in tiles [x y]
                 (rj.c/map->Tile {:x x :y y
                                  :entities [(rj.c/map->Entity {:id   nil
                                                                :type :wall})]})))
       (ring-coords origin dist)))
