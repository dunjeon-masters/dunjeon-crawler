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
                  [x y t]))))
   :rooms []
   :last-add? nil})

(defn print-level
  [{:keys [level rooms] :as LEVEL}]
  (doseq [row level]
    (println
      (map #(case (% 2)
              :w "X"
              :f "_"
              :d "+"
              :t "!")
           row)))
  :as LEVEL)

(defn valid-door-locs
  [x y w h]
  (let [x-off (int (math/ceil (/ (+ -1 x x w) 2)))
        _ (? x-off)
        y-off (int (math/ceil (/ (+ -1 y y h) 2)))
        _ (? y-off)]
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
           [x y :d])})

(defn change-in-level
  [cell level]
  (map #(if (and (= (% 0) (cell 0))
                 (= (% 1) (cell 1)))
          cell %)
       level))

(defn room->points
  [{:keys [x y width height door] :as room}]
  (let [corner? (fn [{:keys [x y width height]} [i j]]
                  (let [x-max (+ -1 x width)
                        y-max (+ -1 y height)]
                    (or (= [i j] [x     y])
                        (= [i j] [x     y-max])
                        (= [i j] [x-max y])
                        (= [i j] [x-max y-max]))))]
    (change-in-level
      door
      (map (fn [[i j _ :as t]]
             (if (or (seq (clojure.set/intersection #{i} #{x (+ -1 x width)}))
                     (seq (clojure.set/intersection #{j} #{y (+ -1 y width)})))
               (assoc t 2 (if (and (not (corner? room [i j]))
                                   (< (rand-int 100) 20)) :t :w)) t))
           (for [i (range x (+ x width))
                 j (range y (+ y height))]
             [i j :f])))))

(defn room-in-level?
  [level {:keys [x y width height]}]
  (let [level-width (count level)
        level-height (count (first level))]
    (and (pos? (inc x))
         (pos? (inc y))
         (<= (+ x width) level-width)
         (<= (+ y height) level-height))))

(defn overlapping?
  [rooms {:keys [x y width height]}]
  (loop [rooms rooms]
    (if (seq rooms)
      (let [test-room (first rooms)
            x-intersecting? (<= (* 2 (math/abs
                                      (- x (:x test-room))))
                               (+ -1 width (:width test-room)))
            y-intersecting? (<= (* 2 (math/abs
                                      (- y (:y test-room))))
                               (+ -1 height (:height test-room)))
            intersect? (and x-intersecting?
                            y-intersecting?)]
        (if intersect?
          true
          (recur (rest rooms))))
      false)))

(defn add-room
  [{:keys [level rooms]} room]
  (println room)
  (let [points (room->points room)
        in-room? (set
                   (map (fn [[x y _]] [x y])
                        points))
        valid-room? (and (room-in-level? level room)
                         (not (overlapping? rooms room)))]
    {:level (if valid-room?
              (reduce (fn [level [x y t]]
                        (if (in-room? [x y])
                          (assoc-in level [y x] [x y t])
                          level))
                      level points)
              level)
     :rooms (if valid-room?
              (conj rooms room)
              rooms)
     :last-add? valid-room?}))

(defn test-rooms
  [[level-x level-y] [room-x room-y] room-size]
  (let [level (gen-level level-x level-y :f)
        room (create-room [room-x room-y] [room-size room-size])]
    (as-> (add-room level room) level
      (add-room level (create-room [0 0] [5 5]))
      (print-level level))))

