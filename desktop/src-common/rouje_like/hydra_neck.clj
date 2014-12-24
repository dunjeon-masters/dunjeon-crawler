(ns rouje-like.hydra-neck
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.tickable :as rj.t]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]
            [clojure.set :refer [union]]))

(declare process-input-tick)

(defn add-hydra-neck
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)
         e-head (first (rj.e/all-e-with-c system :hydra-head))
         c-head-pos (rj.e/get-c-on-e system e-head :position)
         head-pos [(:x c-head-pos) (:y c-head-pos)]
         head-neighbors (rj.u/get-neighbors-of-type world head-pos [:dune])
         n-pos (first head-neighbors)

         get-head-tile (fn [world]
                         (get-in world [(:x n-pos)
                                        (:y n-pos)]
                                 nil))]
     (loop [target-tile (get-head-tile world)]
       (add-hydra-neck system target-tile))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-hydra-neck (br.e/create-entity)
         hp (:hp (rj.cfg/entity->stats :hydra-neck))
         system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(rj.cfg/<walls> (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-hydra-neck
                                                               :type :hydra-neck})))))]
     {:system (rj.e/system<<components
                system e-hydra-neck
                [[:hydra-neck {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :hydra-neck}]
                 [:mobile {:can-move?-fn rj.m/-can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 100}]                     ;;4
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :hydra-neck))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   []
                             :is-valid-target? (partial #{:hydra-head})}]
                 [:destructible {:hp         hp
                                 :max-hp     hp
                                 :def        (:def (rj.cfg/entity->stats :hydra-neck))
                                 :can-retaliate? false
                                 :take-damage-fn rj.d/take-damage
                                 :on-death-fn nil
                                 :status-effects []}]
                 [:energy {:energy 2
                           :default-energy 2}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :hydra-neck))}]
                 [:tickable {:tick-fn process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the hydra's neck")}]])
      :z (:z target-tile)})))

(defn process-input-tick
  [c-this e-this system]
  (if-let [_ (first (rj.e/all-e-with-c system :hydra-head))]
    (rj.t/process-input-tick
      (assoc c-this :target-e :hydra-head) e-this system)
    (as-> system system
      (let [e-world (first (rj.e/all-e-with-c system :world))
            c-position (rj.e/get-c-on-e system e-this :position)]
        (rj.u/update-in-world system e-world
                              [(:z c-position) (:x c-position) (:y c-position)]
                              (fn [entities]
                                (vec
                                  (remove
                                    #(#{e-this} (:id %))
                                    entities)))))
      (rj.e/kill-e system e-this))))
