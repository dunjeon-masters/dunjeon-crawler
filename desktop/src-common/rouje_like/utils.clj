(ns rouje-like.utils
  (:require [clojure.math.numeric-tower :as math]
            [clojure.pprint :refer [pprint]]
            [schema.core :as s]

            [rouje-like.entity-wrapper :as rj.e
             :refer [>?system]]
            [rouje-like.config :as rj.cfg]
            [rouje-like.components :as rj.c
             :refer [->2DPoint ->3DPoint]])
  (:import [rouje_like.components Entity Tile]
           [clojure.lang Fn]
           [java.util UUID]))

#_(in-ns 'rouje-like.utils)
#_(use 'rouje-like.utils :reload)

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

(defmacro as-?>
  [expr & forms]
  (let [temp (gensym)]
    `(let [~@(interleave
               (cycle [temp expr])
               (interleave forms
                           (repeat `(if (nil? ~temp) ~expr ~temp))))]
       ~expr)))

(def cmdl-buffer (atom ""))

(def >?get-pri
  {s/Keyword s/Num})
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

(s/defn ^{:private true
          :always-validate true} sort-by-type
  [entities :- [Entity]
   get-pri :- >?get-pri]
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

(s/defn ^:always-validate
  tile->top-entity
  ([target-tile :- (s/maybe Tile)]
   (tile->top-entity target-tile get-default-pri))
  ([target-tile :- (s/maybe Tile)
    get-pri :- >?get-pri]
   (-> target-tile
       (:entities)
       (sort-by-type get-pri)
       (first))))

(def >?2DVec
  (s/pred #(= 2 (count %)) '>?2DVec))
(def >?3DVec
  (s/pred #(= 3 (count %)) '>?3DVec))
(s/defn ^:always-validate
  taxicab-dist
  [XY :- (s/both >?2DVec [s/Num])
   IJ :- (s/both >?2DVec [s/Num])]
  (let [[x y] XY
        [i j] IJ]
    (+ (math/abs (- i x))
       (math/abs (- j y)))))

(s/defn ^:always-validate
  points->line
  [XY :- (s/both >?2DVec [s/Num])
   IJ :- (s/both >?2DVec [s/Num])]
  (let [[x y] XY
        [i j] IJ
        dx (math/abs (- i x))
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

(def >?level
  (s/pred #(= (rj.c/get-type :tile)
              (type (first (first %))))
          '>?level))
(s/defn ^:always-validate
  can-see?
  [level :- >?level
   sight :- s/Num
   XY :- (s/both >?2DVec [s/Num])
   IJ :- (s/both >?2DVec [s/Num])]
  (if (< sight
         (taxicab-dist XY IJ))
    false
    (as-> (points->line XY IJ) line
      (filter (fn [[x y]]
                (-> (get-in level [x y])
                    (tile->top-entity)
                    (:type)
                    (rj.cfg/<sight-blockers>)))
              line)
      (every? (partial = IJ)
              line))))

(def direction->offset
  {:left  [-1 0]
   :right [1  0]
   :up    [0  1]
   :down  [0 -1]})

(s/defn ^:always-validate
  coords+offset
  "Offset the first coordinate by the second,
  returning the result coordinate."
  [XY   :- >?2DVec
   DXDY :- >?2DVec]
  (let [[x  y]  XY
        [dx dy] DXDY]
    [(+ x dx) (+ y dy)]))

(s/defn ^:always-validate
  get-neighbors-coords
  "Return the coordinates of all neighboring
  (ie: up/down/left/right)
  squares of the given [x y] pos."
  [origin :- >?2DVec]
  (map coords+offset
       (repeat origin) (vals direction->offset)))

(s/defn ^:always-validate
  get-neighbors
  [level :- >?level
   origin :- >?2DVec]
  (map (fn [v] (get-in level v nil))
       (get-neighbors-coords origin)))

(s/defn ^:always-validate
  get-neighbors-of-type
  [level :- >?level
   origin :- >?2DVec
   type :- (s/either #{s/Keyword}
                     [s/Keyword])]
  (->> (get-neighbors level origin)
       (filter #(and (not (nil? %))
                     ((into #{} type) (:type (tile->top-entity %)))))))

(s/defn ^:always-validate
  radial-distance
  [[x1 y1] [x2 y2]]
  (max (math/abs (- x1 x2))
       (math/abs (- y1 y2))))

(s/defn ^:always-validate
  get-entities-radially
  [level         :- >?level
   origin        :- >?2DVec
   within-range? :- Fn]
  (->> (flatten level)
       (filter #(within-range?
                  (radial-distance origin [(:x %) (:y %)])))))

(s/defn ^:always-validate
  get-neighbors-of-type-within
  [level   :- >?level
   origin  :- >?2DVec
   type    :- [s/Keyword]
   dist-fn :- Fn]
  (filter #(and (dist-fn (radial-distance origin [(:x %) (:y %)]))
                ((into #{} type) (:type (tile->top-entity %
                                                          (zipmap (conj type :else)
                                                                  (conj (vec
                                                                          (repeat (count type) 2))
                                                                        1))))))
          (flatten level)))

(s/defn ^:always-validate
  not-any-radially-of-type
  [level   :- >?level
   origin  :- >?2DVec
   dist-fn :- Fn
   type    :- [s/Keyword]]
  (not-any? (fn [tile]
              ((into #{} type) (:type (tile->top-entity tile
                                                      (zipmap (conj type :else)
                                                              (conj (vec
                                                                      (repeat (count type) 2))
                                                                    1))))))
            (get-entities-radially level origin dist-fn)))

(s/defn ^:always-validate
  ring-coords
  [XY   :- >?2DVec
   dist :- s/Num]
  (let [[x y] XY
        ∆x|y (vec (range (- 0 dist) (inc dist)))]
    (for [dx ∆x|y
          dy ∆x|y
          :when (or (if (= dist 1)
                      true)
                    (= dist (math/abs dx))
                    (= dist (math/abs dy)))]
      [(+ x dx) (+ y dy)])))

(s/defn ^:always-validate
  get-ring-around
  [level  :- >?level
   origin :- >?2DVec
   dist   :- s/Num]
  (map (fn [[x y]]
         (get-in level [x y]
                 (rj.c/map->Tile {:x x :y y
                                  :entities [(rj.c/map->Entity {:id   nil
                                                                :type :wall})]})))
       (ring-coords origin dist)))

(s/defn ^:always-validate
  rand-rng
  [start :- s/Num
   end   :- s/Num]
  (+ (rand-int (- (inc end)
                  start))
     start))

(s/defn ^:always-validate
  update-in-world
  [system       :- >?system
   e-world      :- UUID
   point-3D     :- >?3DVec
   fn<-entities :- Fn]
  (rj.e/upd-c system e-world :world
              (fn [c-world]
                (update-in c-world [:levels]
                           (fn [levels]
                             (update-in levels point-3D
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (fn<-entities entities))))))))))

(s/defn ^:always-validate
  change-type
  [system   :- >?system
   e-this   :- UUID
   old-type :- s/Keyword
   new-type :- s/Keyword]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-position (rj.e/get-c-on-e system e-this :position)
        this-pos (->3DPoint c-position)]
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

(s/defn ^:always-validate
  update-gold
  [system :- >?system
   e-this :- UUID
   value  :- s/Num]
  "Update the amount of gold on E-THIS by VALUE."
  (rj.e/upd-c system e-this :wallet
              (fn [c-wallet]
                (update-in c-wallet [:gold]
                           (partial + value)))))

(s/defn ^:always-validate
  inspectable?
  [entity :- Entity]
  (let [type (:type entity)]
    (some #(= type %) rj.cfg/inspectables)))

(def >?levels
  (s/pred #(= (rj.c/get-type :tile)
                (type (first (first (first %)))))
            '>?levels))
(s/defn ^:always-validate
  entities-at-pos
  [levels :- >?levels
   pos    :- >?3DVec]
  "Return the entities of the tile at [Z X Y]."
  (let [target-tile (get-in levels pos :err)]
    (assert (not= :err target-tile))
    (:entities target-tile)))

(s/defn ^:always-validate
  get-closest-tile-to
  [level :- >?level
   this-pos :- (s/both >?2DVec [s/Num])
   target-tile :- Tile]
  (let [target-pos (->2DPoint target-tile)
        dist-from-target (taxicab-dist this-pos target-pos)

        this-pos+dir-offset (fn [this-pos dir]
                              (coords+offset this-pos (direction->offset dir)))
        shuffled-directions (shuffle [:up :down :left :right])
        offset-shuffled-directions (map #(this-pos+dir-offset this-pos %)
                                        shuffled-directions)

        is-valid-target-tile? rj.cfg/<valid-mob-targets>

        nth->offset-pos (fn [index]
                          (nth offset-shuffled-directions index))
        isa-closer-tile? (fn [target-pos+offset]
                           (and (< (taxicab-dist target-pos+offset target-pos)
                                   dist-from-target)
                                (is-valid-target-tile?
                                  (:type (tile->top-entity (get-in level target-pos+offset))))))]
    (cond
      (isa-closer-tile? (nth->offset-pos 0))
      (get-in level (nth->offset-pos 0))

      (isa-closer-tile? (nth->offset-pos 1))
      (get-in level (nth->offset-pos 1))

      (isa-closer-tile? (nth->offset-pos 2))
      (get-in level (nth->offset-pos 2))

      (isa-closer-tile? (nth->offset-pos 3))
      (get-in level (nth->offset-pos 3))

      :else nil)))
