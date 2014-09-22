(ns rouje-like.lichen
  (:import [clojure.lang Atom])
  (:require [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]))

;TODO FIXME
(defn take-damage!
  [system this damage]
  (let [c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system (rj.c/get-type :world)))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage))))
      (-> system
          (rj.e/upd-c e-world :world
                      (fn [c-world]
                        (update-in c-world [(:x c-position) (:y c-position)]
                                   (fn [tile]
                                     (update-in tile [:entities]
                                                (fn [entities]
                                                  (vec (remove #(#{:lichen} (:type %))
                                                               entities))))))))
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

;TODO FIXME
#_(defn add-lichen
  [{:keys [system]}]
  (loop [system system]
    (println "add-lichen: " (type system))
    (let [world (:world (first (rj.e/all-e-with-c system (rj.c/get-type :world))))
          x (+ (rand-int (- (count world) 2)) 1)
          y (+ (rand-int (- (count (first world)) 2)) 1)
          first-open-space (get-empty-neighbor world x y)]
      (if (nil? first-open-space)
        (recur system)
        (let [e-lichen (brute.entity/create-entity)
              new-system (-> system
                             (rj.e/add-e e-lichen)
                             (rj.e/add-c e-lichen (rj.c/map->Lichen {:grow-chance% (atom 10)}))
                             (rj.e/add-c e-lichen (rj.c/map->Position {:world world
                                                                       :x (atom (:x first-open-space))
                                                                       :y (atom (:y first-open-space))}))
                             (rj.e/add-c e-lichen (rj.c/map->Destructible {:hp (atom 1)
                                                                           :defense (atom 1)
                                                                           :take-damage! rouje-like.lichen/take-damage!}))
                             (rj.e/add-c e-lichen (rj.c/map->Tickable {:tick-fn rouje-like.lichen/process-input-tick!
                                                                       :args nil})))
              _ (do (println (brute.entity/get-all-entities-with-component new-system :lichen)))]
          (swap! world (fn [world]
                         (update-in world [(:x first-open-space) (:y first-open-space)]
                                    (fn [tile]
                                      (update-in tile [:entities]
                                                 (fn [entities]
                                                   (conj entities
                                                         (rj.c/map->Entity {:type :lichen
                                                                            :id   e-lichen}))))))))
          new-system)))))

(defn process-input-tick!
  [system this _]
  (let [world (:world (first (rj.e/all-e-with-c system (rj.c/get-type :world))))

        c-position (rj.e/get-c-on-e system this :position)
        x-pos (:x c-position)
        y-pos (:y c-position)

        grow-chance% (:grow-chance% (rj.e/get-c-on-e system this :lichen))
        first-open-space (get-empty-neighbor world x-pos y-pos)
        should-grow (and (not (nil? first-open-space))
                         (< (rand-int 100) grow-chance%))]
    (do (println "tick lichen"))
    (if should-grow
      (println "growing")
      #_(add-lichen {:system system
                   :world world})
      system)))