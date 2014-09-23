(ns rouje-like.lichen
  (:import [clojure.lang Atom])
  (:require [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]
            [brute.entity :as br.e]
            [clojure.pprint :refer [pprint]]))

(defn take-damage
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

(defn get-neighbors
  [world origin]
  (map (fn [vec] (get-in world vec nil))
       (get-neighbors-coords origin)))

(defn get-neighbors-of-type
  [world origin type]
  (->> (get-neighbors world origin)
       (filter #(and (not (nil? %))
                     ((into #{} type)
                      (-> % (:entities)
                          (rj.c/sort-by-pri)
                          (first) (:type)))))))

(declare process-input-tick)

(defn add-lichen
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)]
     ;; TODO: Only add lichen if !:wall, use remove
     #_(rand-nth (remove #(#{:wall}
                           (-> % (:entities)
                               (rj.c/sort-by-pri)
                               (first) (:type)))
                         world))
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
                                                              (vec (conj (remove #(#{:wall :floor} (:type %)) entities)
                                                                         (rj.c/map->Entity {:id nil
                                                                                            :type :floor})
                                                                         (rj.c/map->Entity {:id   e-lichen
                                                                                            :type :lichen})))))))))))
         (rj.e/add-c e-lichen (rj.c/map->Lichen {:grow-chance% 7
                                                 :max-blob-size 10}))
         (rj.e/add-c e-lichen (rj.c/map->Position {:x (:x target)
                                                   :y (:y target)}))
         (rj.e/add-c e-lichen (rj.c/map->Destructible {:hp      1
                                                       :defense 1
                                                       :take-damage take-damage}))
         (rj.e/add-c e-lichen (rj.c/map->Tickable {:tick-fn process-input-tick
                                                   :args    nil}))))))

(defn get-size-of-lichen-blob
  [world origin]
  (loop [current (get-in world origin)
         explored #{}
         un-explored (into #{} (get-neighbors-of-type world origin [:lichen]))]
    (if (empty? un-explored)
      (count explored)
      (recur (first un-explored)
             (conj explored current)
             (into (rest un-explored)
                   (remove #(or (#{current} %)
                                (explored %))
                           (get-neighbors-of-type world
                                                  [(:x (first un-explored))
                                                   (:y (first un-explored))]
                                                  [:lichen])))))))

(defn process-input-tick
  [system this _]
  (let [c-position (rj.e/get-c-on-e system this :position)
        x (:x c-position)
        y (:y c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        c-lichen (rj.e/get-c-on-e system this :lichen)
        grow-chance% (:grow-chance% c-lichen)
        max-blob-size (:max-blob-size c-lichen)

        empty-neighbors (get-neighbors-of-type world [x y]
                                               [:floor :torch :gold])]
    (if (and (seq empty-neighbors)
             (< (rand 100) grow-chance%)
             (< (get-size-of-lichen-blob world [x y])
                max-blob-size))
      (add-lichen system (rand-nth empty-neighbors))
      system)))