(ns rouje-like.hydra-tail
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c
             :refer [can-move? move
                     can-attack? attack
                     ->3DPoint]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.tickable :as rj.t]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]
            [clojure.set :refer [union]]))

(declare process-input-tick)

(defn add-hydra-tail
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)
         e-neck (first (rj.e/all-e-with-c system :hydra-neck))
         c-neck-pos (rj.e/get-c-on-e system e-neck :position)
         neck-pos [(:x c-neck-pos) (:y c-neck-pos)]
         neck-neighbors (rj.u/get-neighbors-of-type world neck-pos [:dune])
         n-pos (first neck-neighbors)

         get-neck-tile (fn [world]
                         (get-in world [(:x n-pos)
                                        (:y n-pos)]))]
     (loop [target-tile (get-neck-tile world)]
       (add-hydra-tail system target-tile))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-hydra-tail (br.e/create-entity)
         hp (:hp (rj.cfg/entity->stats :hydra-tail))
         system (rj.u/update-in-world system e-world
                                      (->3DPoint target-tile)
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #( rj.cfg/<walls> (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-hydra-tail
                                                               :type :hydra-tail})))))]
     {:system (rj.e/system<<components
                system e-hydra-tail
                [[:hydra-tail {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :hydra-tail}]
                 [:mobile {:can-move?-fn rj.m/-can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 100}]                     ;;4
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :hydra-tail))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:hydra-neck})}]
                 [:destructible {:hp         hp
                                 :max-hp     hp
                                 :def        (:def (rj.cfg/entity->stats :hydra-tail))
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :on-death-fn nil
                                 :status-effects []}]
                 [:energy {:energy 2
                           :default-energy 2}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :hydra-tail))}]
                 [:tickable {:tick-fn process-input-tick
                             :extra-tick-fn nil
                             :pri -1}]
                 [:broadcaster {:name-fn (constantly "the hydra's tail")}]])
      :z (:z target-tile)})))

(defn process-input-tick
  [c-this e-this system]
  (if-let [_ (first (rj.e/all-e-with-c system :hydra-neck))]
    (rj.t/process-input-tick
      (assoc c-this :target-e :hydra-neck) e-this system)
    (as-> system system
      (let [e-world (first (rj.e/all-e-with-c system :world))
            c-position (rj.e/get-c-on-e system e-this :position)]
        (rj.u/update-in-world system e-world
                              (->3DPoint c-position)
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities)))))
      (rj.e/kill-e system e-this))))
