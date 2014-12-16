(ns rouje-like.bat
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c :refer [can-move? move]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.destructible :as rj.d]
            [rouje-like.mobile :as rj.m]
            [rouje-like.config :as rj.cfg]))

(declare process-input-tick)

(defn add-bat
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)

         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (rj.cfg/<floors> (:type (rj.u/tile->top-entity target-tile)))
         (add-bat system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-bat (br.e/create-entity)
         hp (:hp (:bat rj.cfg/entity->stats))
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-bat
                                                               :type :bat})))))]
     {:system (rj.e/system<<components
                system e-bat
                [[:bat {}]
                 [:position {:x (:x target-tile)
                             :y (:y target-tile)
                             :z (:z target-tile)
                             :type :bat}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:destructible {:hp  hp
                                 :max-hp hp
                                 :def (:def (:bat rj.cfg/entity->stats))
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :status-effects []}]
                 [:tickable {:tick-fn process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the bat")}]])
      :z (:z target-tile)})))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        world (nth levels (:z c-position))

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
