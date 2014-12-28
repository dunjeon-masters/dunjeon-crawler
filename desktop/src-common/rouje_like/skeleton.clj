(ns rouje-like.skeleton
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.tickable :as rj.t]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack
                                                    ->3DPoint ->2DPoint]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.destructible :as rj.d]
            [rouje-like.utils :as rj.u]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]
            [clojure.set :refer [union]]))

#_(use 'rouje-like.skeleton :reload)

(defn add-skeleton
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
         (add-skeleton system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-skeleton (br.e/create-entity)
         hp (:hp (rj.cfg/entity->stats :skeleton))
         system (rj.u/update-in-world system e-world
                                      [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-skeleton
                                                               :type :skeleton})))))]
     {:system (rj.e/system<<components
                system e-skeleton
                [[:skeleton {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :skeleton}]
                 [:mobile {:can-move?-fn (fn [c-mobile e-this t-tile system]
                                           (rj.m/can-move? c-mobile e-this t-tile system))
                           :move-fn      rj.m/move}]
                 [:sight {:distance 4}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :skeleton))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp             hp
                                 :max-hp         hp
                                 :def            (:def (rj.cfg/entity->stats :skeleton))
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :on-death-fn    nil
                                 :status-effects []}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :skeleton))}]
                 [:energy {:energy 1
                           :default-energy 1}]
                 [:tickable {:tick-fn rj.t/process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the skeleton")}]])
      :z (:z target-tile)})))
