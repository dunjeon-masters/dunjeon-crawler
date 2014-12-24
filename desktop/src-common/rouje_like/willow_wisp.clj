(ns rouje-like.willow-wisp
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.status-effects :as rj.stef]))

(declare process-input-tick)

(defn add-willow-wisp
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
         (add-willow-wisp system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-willow-wisp (br.e/create-entity)
         system (rj.u/update-in-world system e-world
                                      [(:z target-tile) (:x target-tile) (:y target-tile)]
                                      (fn [entities]
                                        (vec
                                          (conj
                                            (remove #(#{:wall} (:type %)) entities)
                                            (rj.c/map->Entity {:id   e-willow-wisp
                                                               :type :willow-wisp})))))]
     {:system (rj.e/system<<components
                system e-willow-wisp
                [[:willow-wisp {}]
                 [:position {:x    (:x target-tile)
                             :y    (:y target-tile)
                             :z    (:z target-tile)
                             :type :willow-wisp}]
                 [:mobile {:can-move?-fn rj.m/can-move?
                           :move-fn      rj.m/move}]
                 [:sight {:distance 4}]
                 [:attacker {:atk              (:atk (rj.cfg/entity->stats :willow-wisp))
                             :can-attack?-fn   rj.atk/can-attack?
                             :attack-fn        rj.atk/attack
                             :status-effects   [{:type :burn
                                                 :duration 2
                                                 :value 1
                                                 :e-from e-willow-wisp
                                                 :apply-fn rj.stef/apply-burn}]
                             :is-valid-target? (partial #{:player})}]
                 [:destructible {:hp         (:hp  (rj.cfg/entity->stats :willow-wisp))
                                 :max-hp     (:hp  (rj.cfg/entity->stats :willow-wisp))
                                 :def        (:def (rj.cfg/entity->stats :willow-wisp))
                                 :can-retaliate? false
                                 :on-death-fn nil
                                 :status-effects []
                                 :take-damage-fn rj.d/take-damage}]
                 [:killable {:experience (:exp (rj.cfg/entity->stats :willow-wisp))}]
                 [:tickable {:tick-fn rj.t/process-input-tick
                             :pri 0}]
                 [:broadcaster {:name-fn (constantly "the will-o-wisp")}]])
      :z (:z target-tile)})))
