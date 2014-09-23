(ns rouje-like.bat
  (:require [rouje-like.entity :as rj.e]
            [brute.entity :as br.e]
            [rouje-like.components :as rj.c]))

(declare process-input-tick!)

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
                                                               (vec (remove #(#{:bat} (:type %))
                                                                            entities))))))))))
          (rj.e/kill-e this)))))

(defn add-bat
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)]
     (add-bat system (get-in world [(rand-int (count world))
                                    (rand-int (count (first world)))]))))
  ([system target]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-bat (br.e/create-entity)]
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
                                                                         (rj.c/map->Entity {:id   e-bat
                                                                                            :type :bat})))))))))))
         (rj.e/add-c e-bat (rj.c/map->Bat {}))
         (rj.e/add-c e-bat (rj.c/map->Position {:x (:x target)
                                                :y (:y target)}))
         (rj.e/add-c e-bat (rj.c/map->Destructible {:hp      1
                                                    :defense 1
                                                    :take-damage take-damage}))
         (rj.e/add-c e-bat (rj.c/map->Tickable {:tick-fn process-input-tick!
                                                :args    nil}))))))

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

(defn process-input-tick!
  [system this _]
  (let [c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        valid-targets (get-neighbors-of-type world [(:x c-position) (:y c-position)]
                                             [:floor :gold :torch])
        target (if (seq valid-targets)
                 (rand-nth valid-targets)
                 nil)

        valid-targets-d2 (if (not (nil? target))
                           (get-neighbors-of-type world [(:x target) (:y target)]
                                                  [:floor :gold :torch])
                           [])

        target-d2 (if (seq valid-targets-d2)
                    (rand-nth valid-targets-d2)
                    nil)]
    (if (not (nil? target-d2))
      (-> system
        (rj.e/upd-c e-world :world
                    (fn [c-world]
                      (update-in c-world [:world]
                                 (fn [world]
                                   (let [bat-pos [(:x c-position) (:y c-position)]
                                         target-pos [(:x target-d2) (:y target-d2)]]
                                     (-> world
                                         (update-in target-pos
                                                    (fn [tile]
                                                      (update-in tile [:entities]
                                                                 (fn [entities]
                                                                   (vec (conj entities
                                                                              (rj.c/map->Entity {:type :bat
                                                                                                 :id   this})))))))
                                         (update-in bat-pos
                                                    (fn [tile]
                                                      (update-in tile [:entities]
                                                                 (fn [entities]
                                                                   (vec (remove #(#{:bat} (:type %))
                                                                                entities))))))))))))

        (rj.e/upd-c this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target-d2))
                          (assoc-in [:y] (:y target-d2))))))
      system)))