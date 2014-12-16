(ns rouje-like.rooms
  (:import [clojure.lang PersistentArrayMap PersistentVector])
  (:require [rouje-like.utils :refer [?]]
            [clojure.math.numeric-tower :as math]))

#_(in-ns 'rouje-like.rooms)
#_(use 'rouje-like.rooms :reload)

(defn gen-level
  [width height t]
  {:level (vec
            (for [x (range width)]
              (vec
                (for [y (range height)]
                  [x y t {}]))))
   :rooms []
   :last-add? nil})

(defn print-level
  [{:keys [level rooms] :as LEVEL}]
  (println " " (take (count level)
                 (range)))
  (doseq [row level
          :let [r (.indexOf level row)]]
    (println
      r (map #(case (% 2)
                :w  "X"
                :f  "_"
                :d  "+"
                :at "!"
                :st "v"
                "Q")
             row)))
  LEVEL)

(defmulti flatten-level type)
(defmethod flatten-level PersistentArrayMap
  [level]
  (flatten-level (:level level)))
(defmethod flatten-level PersistentVector
  [level]
  (vec (map vec (partition 4 (flatten level)))))

(defn valid-door-locs
  [x y w h]
  (let [x-off (int (math/ceil (/ (+ -1 x x w) 2)))
        y-off (int (math/ceil (/ (+ -1 y y h) 2)))]
    [[x          y-off]
     [x-off      y]
     [x-off      (+ -1 y h)]
     [(+ -1 x w) y-off]]))

(defn create-room
  [[x y] [width height]]
  {:x x :y y
   :width width
   :height height
   :door (let [[x y] (rand-nth (valid-door-locs x y width height))]
           [x y :d {}])})

(defn change-in-level
  [cell level]
  (map #(if (and (= (% 0) (cell 0))
                 (= (% 1) (cell 1)))
          cell %)
       level))

(defn room->points
  [{:keys [x y width height door] :as room}]
  (let [num-spike-traps (atom 2)
        placed-traps (atom {:top false
                            :btm false
                            :left false
                            :right false})]
    (letfn [(trap->dir [[i j _ _] {:keys [x y width height]}]
              (let [top   y
                    left  x
                    right (+ x width  -1)
                    btm   (+ y height -1)]
                (cond
                  (= i left)  :right
                  (= j top)   :down
                  (= i right) :left
                  (= j btm)   :up)))
            (corner? [{:keys [x y width height]} [i j _ _]]
              (let [x-max (+ -1 x width)
                    y-max (+ -1 y height)]
                (or (= [i j] [x     y])
                    (= [i j] [x     y-max])
                    (= [i j] [x-max y])
                    (= [i j] [x-max y-max]))))
            (point->side [{:keys [x y width height] :as room} [i j _ _]]
              (if (not (corner? room [i j _ _]))
                (cond
                  (= i x)
                  :left
                  (= i (+ -1 x width))
                  :right
                  (= j y)
                  :top
                  (= j (+ -1 y height))
                  :btm)
                nil))
            (edge? [{:keys [x y width height] :as room} [i j _ _ ]]
              (or (= i x) (= i (+ -1 x width))
                  (= j y) (= j (+ -1 y height))))
            (wall-ify [cell]
              (if (edge? room cell)
                (assoc cell 2 :w)
                cell))
            (arrow-trap-ify [cell]
              (if-let [side (point->side room cell)]
                (if (and (not (@placed-traps side))
                         (not= (cell 2) :d))
                  (do (swap! placed-traps #(assoc % side true))
                      (assoc cell
                             2 :at
                             3 {:dir (trap->dir cell room)}))
                  cell)
                cell))
            (spike-trap-ify [cell]
              (if (and (pos? @num-spike-traps)
                       (not (edge? room cell)))
                (do (swap! num-spike-traps dec)
                    (assoc cell
                           2 :st))
                cell))
            (door-ify [door points]
              (change-in-level door points))]
      (as-> (for [i (range x (+ x width))
                  j (range y (+ y height))]
              [i j :f {}]) points
        (map wall-ify points)
        (door-ify door points)
        (map arrow-trap-ify (shuffle points))
        (map spike-trap-ify (shuffle points))))))

(defn room-in-level?
  [level {:keys [x y width height]}]
  (let [level-width (count level)
        level-height (count (first level))]
    (and (pos? x)
         (pos? y)
         (< (+ x width) level-width)
         (< (+ y height) level-height))))

(defn overlapping?
  [rooms {:keys [x y width height]}]
  (loop [rooms rooms]
    (if (seq rooms)
      (let [test-room (first rooms)
            x-intersecting? (<= (* 2 (math/abs
                                      (- x (:x test-room))))
                                ;+ 2 so as to have a 1 thickness padd around the room
                               (+ 2 -1 width (:width test-room)))
            y-intersecting? (<= (* 2 (math/abs
                                      (- y (:y test-room))))
                                ;+ 2 so as to have a 1 thickness padd around the room
                               (+ 2 -1 height (:height test-room)))
            intersect? (and x-intersecting?
                            y-intersecting?)]
        (if intersect?
          true
          (recur (rest rooms))))
      false)))

(defn add-room
  [{:keys [level rooms]} room]
  (let [points (room->points room)
        in-room? (set
                   (map (fn [[x y _ _]] [x y])
                        points))
        valid-room? (and (room-in-level? level room)
                         (not (overlapping? rooms room)))]
    {:level (if valid-room?
              (reduce (fn [level [x y t m]]
                        (if (in-room? [x y])
                          (assoc-in level [y x] [x y t m])
                          level))
                      level points)
              level)
     :rooms (if valid-room?
              (conj rooms room)
              rooms)
     :last-add? valid-room?}))

(defn gen-level-with-rooms
  [width height number-of-rooms room-size]
  (assert (> (* width height 1/3)
             (* number-of-rooms room-size room-size)))
  (letfn [(rand-pos [] [(rand-int width) (rand-int height)])]
    (loop [level (gen-level width height :f)
           rooms-left number-of-rooms]
      (if (pos? (? rooms-left))
        (if (and (:last-add? level)
                 (? (= (? (count (:rooms level)))
                       (? (- number-of-rooms rooms-left -1)))))
          (recur (add-room
                   level
                   (create-room (rand-pos) [room-size room-size]))
                 (dec rooms-left))
          (recur (add-room
                   level
                   (create-room (rand-pos) [room-size room-size]))
                 rooms-left))
        level))))

