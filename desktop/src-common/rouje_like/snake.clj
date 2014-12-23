(ns rouje-like.snake
  (:require [brute.entity :as br.e]
            [clojure.set :refer [union]]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u
             :refer [?]]
            [rouje-like.components :as rj.c
             :refer [can-move? move
                     can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]))

(defn add-snake
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
         (add-snake system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-snake (br.e/create-entity)
         hp (:hp (rj.cfg/entity->stats :snake))
         system (rj.u/update-in-world system e-world
                                      [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-snake
                                                               :type :snake})))))]
     {:system (rj.e/system<<components
                system e-snake
                [[:snake {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :snake}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 4}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :snake))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   [{:type :poison
                                                 :duration 4
                                                 :value 2
                                                 :e-from e-snake
                                                 :apply-fn rj.stef/apply-poison}]
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp         hp
                                 :max-hp     hp
                                 :def        (:def (rj.cfg/entity->stats :snake))
                                 :can-retaliate? false
                                 :status-effects []
                                 :on-death-fn nil
                                 :take-damage-fn rj.d/take-damage}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :snake))}]
                 [:energy {:energy 2
                           :default-energy 2}]
                 [:tickable {:tick-fn rj.t/process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the snake")}]])
      :z (:z target-tile)})))
