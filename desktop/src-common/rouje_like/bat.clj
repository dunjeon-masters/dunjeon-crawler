(ns rouje-like.bat
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c :refer [can-move? move]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]
            [rouje-like.destructible :as rj.d]
            [rouje-like.mobile :as rj.m]))

(declare process-input-tick)

(defn add-bat
  ([system]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         world (:world c-world)
         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (#{:floor} (:type (rj.u/tile->top-entity target-tile)))
         (add-bat system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-bat (br.e/create-entity)]
     (-> system
         (rj.wr/update-in-world e-world [(:x target-tile) (:y target-tile)]
                                (fn [entities]
                                  (vec
                                    (conj
                                      (remove #(#{:wall} (:type %)) entities)
                                      (rj.c/map->Entity {:id   e-bat
                                                         :type :bat})))))
         (rj.e/add-c e-bat (rj.c/map->Bat {}))
         (rj.e/add-c e-bat (rj.c/map->Position {:x (:x target-tile)
                                                :y (:y target-tile)
                                                :type :bat}))
         (rj.e/add-c e-bat (rj.c/map->Mobile {:can-move?-fn rj.m/can-move?
                                              :move-fn      rj.m/move}))
         (rj.e/add-c e-bat (rj.c/map->Destructible {:hp      1
                                                    :defense 1
                                                    :can-retaliate? false
                                                    :take-damage-fn rj.d/take-damage}))
         (rj.e/add-c e-bat (rj.c/map->Tickable {:tick-fn process-input-tick
                                                :pri 0}))
         (rj.e/add-c e-bat (rj.c/map->Broadcaster {:msg-fn (constantly "the bat")}))))))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)

        neighbor-tiles (rj.u/get-neighbors world [(:x c-position) (:y c-position)])

        target-tile (if (seq neighbor-tiles)
                 (rand-nth (conj neighbor-tiles nil))
                 nil)]
    (if (not (nil? target-tile))
      (cond
        (can-move? c-mobile e-this target-tile system)
        (move c-mobile e-this target-tile system)

        :else system)
      system)))
