(ns rouje-like.lichen
  (:import [clojure.lang Atom])
  (:require [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]
            [brute.entity :as br.e]))

(defn take-damage!
  [system this damage]
  (let [c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage))))
      (-> system
          (rj.e/upd-c e-world :world
                      (fn [c-world]
                        (update-in c-world [:world]
                                   (fn [world]
                                     (update-in world [(:x c-position) (:y c-position)]
                                                (fn [tile]
                                                  (update-in tile [:entities]
                                                             (fn [entities]
                                                               (vec (remove #(#{:lichen} (:type %))
                                                                            entities))))))))))
          (rj.e/kill-e this)))))

(def directions
  {:left  [-1 0]
   :right [1 0]
   :up    [0 1]
   :down  [0 -1]})

(defn offset-coords
  "Offset the starting coordinate by the given amount, returning the result coordinate."
  [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])

(defn get-neighbors-coords
  "Return the coordinates of all neighboring squares of the given coord."
  [origin]
  (map offset-coords (repeat origin) (vals directions)))

(defn get-empty-neighbor
  [world x y]
  (let [neighbors (map (fn [vec] (get-in world vec nil))
                       (get-neighbors-coords [x y]))
        empty-neighbors (filter #(and (not (nil? %))
                                      (#{:floor :gold :torch} (-> %
                                                                  (:entities)
                                                                  (rj.c/sort-by-pri)
                                                                  (first)
                                                                  (:type))))
                                neighbors)]
    (if (empty? empty-neighbors)
      nil
      (rand-nth empty-neighbors))))

(declare process-input-tick!)

(defn add-lichen
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)]
     (add-lichen system (get-in world [(rand-int (count world))
                                     (rand-int (count (first world)))]))))
  ([system target]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-lichen (br.e/create-entity)]
     (-> system
         (rj.e/upd-c e-world :world
                     (fn [c-world]
                       (update-in c-world [:world]
                                  (fn [world]
                                    (update-in world [(:x target) (:y target)]
                                               (fn [tile]
                                                 (update-in tile [:entities]
                                                            (fn [entities]
                                                              (vec (conj entities
                                                                         (rj.c/map->Entity {:id   e-lichen
                                                                                            :type :lichen})))))))))))
         (rj.e/add-c e-lichen (rj.c/map->Lichen {:grow-chance% 1}))
         (rj.e/add-c e-lichen (rj.c/map->Position {:x (:x target)
                                                   :y (:y target)}))
         (rj.e/add-c e-lichen (rj.c/map->Destructible {:hp      1
                                                       :defense 1
                                                       :take-damage! take-damage!}))
         (rj.e/add-c e-lichen (rj.c/map->Tickable {:tick-fn process-input-tick!
                                                   :args    nil}))))))

(defn process-input-tick!
  [system this _]
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        c-position (rj.e/get-c-on-e system this :position)
        x-pos (:x c-position)
        y-pos (:y c-position)

        grow-chance% (:grow-chance% (rj.e/get-c-on-e system this :lichen))
        first-open-space (get-empty-neighbor world x-pos y-pos)
        should-grow (and (not (nil? first-open-space))
                         (< (rand-int 100) grow-chance%))]
    (if should-grow
      (add-lichen system first-open-space)
      system)))