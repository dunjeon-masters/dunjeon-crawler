(ns rouje-like.lichen
  (:import [clojure.lang Atom])
  (:require [brute.entity :as br.e]
            [clojure.pprint :refer [pprint]]

            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]))

(declare process-input-tick)

(defn add-lichen
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)

         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (#{:floor} (:type (rj.u/tile->top-entity target-tile)))
         (add-lichen system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-lichen (br.e/create-entity)]
     {:system (-> system
                  (rj.u/update-in-world e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                         (fn [entities]
                                           (vec (conj (remove #(#{:wall} (:type %)) entities)
                                                      (rj.c/map->Entity {:id   e-lichen
                                                                         :type :lichen})))))
                  (rj.e/add-c e-lichen (rj.c/map->Lichen {:grow-chance% 4
                                                          :max-blob-size 8}))
                  (rj.e/add-c e-lichen (rj.c/map->Position {:x (:x target-tile)
                                                            :y (:y target-tile)
                                                            :z (:z target-tile)                
                                                            :type :lichen}))
                  (rj.e/add-c e-lichen (rj.c/map->Destructible {:hp      1
                                                                :defense 1
                                                                :can-retaliate? true
                                                                :take-damage-fn rj.d/take-damage}))
                  (rj.e/add-c e-lichen (rj.c/map->Attacker {:atk (/ 1 4)
                                                            :can-attack?-fn   rj.atk/can-attack?
                                                            :attack-fn        rj.atk/attack
                                                            :is-valid-target? (constantly true)}))
                  (rj.e/add-c e-lichen (rj.c/map->Tickable {:tick-fn process-input-tick
                                                            :pri 0}))
                  (rj.e/add-c e-lichen (rj.c/map->Broadcaster {:msg-fn (constantly "the lichen")})))
      :z (:z target-tile)})))

(defn get-size-of-lichen-blob
  [world origin]
  (loop [current (get-in world origin)
         explored #{}
         un-explored (into #{} (rj.u/get-neighbors-of-type world origin [:lichen]))]
    (if (empty? un-explored)
      (count explored)
      (recur (first un-explored)
             (conj explored current)
             (into (rest un-explored)
                   (remove #(or (#{current} %)
                                (explored %))
                           (rj.u/get-neighbors-of-type world
                                                  [(:x (first un-explored))
                                                   (:y (first un-explored))]
                                                  [:lichen])))))))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        x (:x c-position)
        y (:y c-position)
        z (:z c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        world (nth levels z)

        c-lichen (rj.e/get-c-on-e system e-this :lichen)
        grow-chance% (:grow-chance% c-lichen)
        max-blob-size (:max-blob-size c-lichen)

        empty-neighbors (rj.u/get-neighbors-of-type world [x y]
                                                    [:floor :torch :gold])]
    (if (and (seq empty-neighbors)
             (< (rand 100) grow-chance%)
             (< (get-size-of-lichen-blob world [x y])
                max-blob-size))
      (do (println :grow-lichen) 
          (:system (add-lichen system (rand-nth empty-neighbors))))
      system)))
