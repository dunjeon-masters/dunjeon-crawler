(ns rouje-like.utils
  (:require [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer [pprint]]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.config :as rj.cfg]
            [rouje-like.components :as rj.c]))

#_(in-ns 'rouje-like.utils)
#_(use 'rouje-like.utils :reload)

(def cli (atom ""))
(def cli? (atom false))

(def get-default-pri
  {:floor 0
   :forest-floor 0
   :dune 0
   :open-door 1
   :health-potion 2
   :magic-potion 2
   :equipment 2
   :torch 3
   :gold 4
   :wall 5
   :tree 5
   :lichen 6
   :bat 7
   :skeleton 8
   :door 9
   :else 99
   :player 100})

(defn- sort-by-type
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

(defn tile->top-entity
  ([target-tile]
   (tile->top-entity target-tile get-default-pri))
  ([target-tile get-pri]
   (-> target-tile
       (:entities)
       (sort-by-type get-pri)
       (first))))

(defn taxicab-dist
  [[x y] [i j]]
  (+ (math/abs (- i x))
     (math/abs (- j y))))

(defn points->line
  [[x y] [i j]]
  (let [dx (math/abs (- i x))
        dy (math/abs (- j y))
        sx (if (< x i) 1 -1)
        sy (if (< y j) 1 -1)
        err (- dx dy)]
    (loop [points []
           x x
           y y
           err err]
      (if (and (= x i) (= y j))
        (conj points
              [x y])
        (let [e2 (* err 2)]
          (recur (conj points [x y])
                 (if (> e2 (- dx))
                   (+ x sx)
                   x)
                 (if (< e2 dx)
                   (+ y sy)
                   y)
                 (if (> e2 (- dx))
                   (if (< e2 dx)
                     (+ (- err dy) dx)
                     (- err dy))
                   (if (< e2 dx)
                     (+ err dx)
                     err))))))))

(defn can-see?
  [level sight [x y] [i j]]
  (let [result (if (< sight
                      (taxicab-dist [x y] [i j]))
                 false
                 (as-> (points->line [x y] [i j]) line
                   (filter (fn [[x y]]
                             (-> (get-in level [x y])
                                 (tile->top-entity)
                                 (:type)
                                 (rj.cfg/<sight-blockers>)))
                           line)
                   (every? (partial = [i j])
                           line)))]
    result))

(def direction->offset
  {:left  [-1 0]
   :right [1  0]
   :up    [0  1]
   :down  [0 -1]})

(defn coords+offset
  "Offset the first coordinate by the second,
  returning the result coordinate."
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn get-neighbors-coords
  "Return the coordinates of all neighboring
  (ie: up/down/left/right)
  squares of the given [x y] pos."
  [origin]
  (map coords+offset
       (repeat origin) (vals direction->offset)))

(defn get-neighbors
  [world origin]
  (map (fn [v] (get-in world v nil))
       (get-neighbors-coords origin)))

(defn get-neighbors-of-type
  [world origin type]
  (->> (get-neighbors world origin)
       (filter #(and (not (nil? %))
                     ((into #{} type) (:type (tile->top-entity %)))))))

(defn radial-distance
  [[x1 y1] [x2 y2]]
  (max (math/abs (- x1 x2))
       (math/abs (- y1 y2))))

(defn get-entities-radially
  [world origin within-range?]
  (->> (flatten world)
       (filter #(within-range?
                  (radial-distance origin [(:x %) (:y %)])))))

(defn get-neighbors-of-type-within
  [world origin type dist-fn]
  (filter #(and (dist-fn (radial-distance origin [(:x %) (:y %)]))
                ((into #{} type) (:type (tile->top-entity %
                                                          (zipmap (conj type :else)
                                                                  (conj (vec
                                                                          (repeat (count type) 2))
                                                                        1))))))
          (flatten world)))

(defn not-any-radially-of-type
  [world origin dist-fn type]
  (not-any? (fn [tile]
              ((into #{} type) (:type (tile->top-entity tile
                                                      (zipmap (conj type :else)
                                                              (conj (vec
                                                                      (repeat (count type) 2))
                                                                    1))))))
            (get-entities-radially world origin dist-fn)))

(defn ring-coords
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

(defn rand-rng
  [start end]
  (+ (rand-int (- (inc end) start)) start))

(defmacro ?
  ([x]
   (let [line (:line (meta &form))
         file *file*]
     `(let [x#   ~x]
        (println (str (str "#" "RJ" " ")
                      "@[" (apply str (drop 5 (str (System/currentTimeMillis))))
                      ", " ~file ":" ~line "]:\n"
                      "\t" (pr-str '~x) "=" (pr-str x#) "\n"))
        x#)))
  ([tag x]
   (let [line (:line (meta &form))
         file *file*]
     `(let [x#   ~x
            tag# ~tag]
        (println (str (str "#" tag# " ")
                      "@[" (apply str (drop 5 (str (System/currentTimeMillis))))
                      ", " ~file ":" ~line "]:\n"
                      "\t" (pr-str '~x) "=" (pr-str x#) "\n"))
        x#))))

(defn update-in-world
  [system e-world [z x y] fn<-entities]
  (rj.e/upd-c system e-world :world
              (fn [c-world]
                (update-in c-world [:levels]
                           (fn [levels]
                             (update-in levels [z x y]
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (fn<-entities entities))))))))))

(defn change-type
  [system e-this old-type new-type]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-position (rj.e/get-c-on-e system e-this :position)
        this-pos [(:z c-position) (:x c-position) (:y c-position)]]
    (as-> system system
      (update-in-world system e-world this-pos
                       (fn [entities]
                         (map #(if (= old-type (:type %))
                                 (assoc % :type new-type)
                                 %)
                              entities)))
      (rj.e/upd-c system e-this :position
                  (fn [c-position]
                    (assoc c-position :type new-type))))))

(defn update-gold
  [system e-this value]
  "Update the amount of gold on E-THIS by VALUE."
  (rj.e/upd-c system e-this :wallet
                          (fn [c-wallet]
                            (update-in c-wallet [:gold]
                                       (partial + value)))))

(defn inspectable?
  [entity]
  (let [type (:type entity)]
    (some #(= type %) rj.cfg/inspectables)))

(defn entities-at-pos
  [level pos]
  "Return the entities of the tile at [Z X Y]."
  (let [target-tile (get-in level pos :err)]
    (assert (not= :err target-tile))
    (:entities target-tile)))
