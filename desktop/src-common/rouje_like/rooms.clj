(ns rouje-like.rooms
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
                :w "X"
                :f "_"
                :d "+"
                :t "!")
             row)))
  :as LEVEL)

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
  (letfn [(trap->dir [[i j] {:keys [x y width height]}]
            (let [top   y
                  left  x
                  right (+ x width  -1)
                  btm   (+ y height -1)]
              (cond
                (= i left)  :right
                (= j top)   :down
                (= i right) :left
                (= j btm)   :up)))
          (corner? [{:keys [x y width height]} [i j]]
            (let [x-max (+ -1 x width)
                  y-max (+ -1 y height)]
              (or (= [i j] [x     y])
                  (= [i j] [x     y-max])
                  (= [i j] [x-max y])
                  (= [i j] [x-max y-max]))))]
    (change-in-level
      door
      (map (fn [[i j _ :as c]]
             (if (or (seq (clojure.set/intersection #{i} #{x (+ -1 x width)}))
                     (seq (clojure.set/intersection #{j} #{y (+ -1 y width)})))
               (if (and (not (corner? room [i j]))
                        (< (rand-int 100) 20))
                 (assoc c
                        2 :t
                        3 {:dir (trap->dir [i j] room)})
                 (assoc c 2 :w))
               c))
           (for [i (range x (+ x width))
                 j (range y (+ y height))]
             [i j :f {}])))))

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
  (let [rand-pos (fn [] [(rand-int width) (rand-int height)])]
    (loop [level (gen-level width height :f)
           i number-of-rooms]
      (if (pos? i)
        (if (:last-add? level)
          (recur (add-room
                   level
                   (create-room (rand-pos) [room-size room-size ]))
                 (dec i))
          (recur (add-room
                   level
                   (create-room (rand-pos) [room-size room-size]))
                 i))
        level))))

(defn test-rooms
  [[level-x level-y] [room-x room-y] room-size]
  (let [level (gen-level level-x level-y :f)
        room (create-room [room-x room-y] [room-size room-size])]
    (as-> (add-room level room) level
      (add-room level (create-room [0 0] [5 5]))
      (print-level level))))

#_(filter #(= :t (% 2)) (map vec (partition 4 (flatten (:level (test-rooms [10 10] [5 5] 5))))))
