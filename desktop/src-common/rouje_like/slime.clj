(ns rouje-like.slime
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.status-effects :as rj.stef]))

(defentity slime
  [[:slime {}]
   [:position {:x    nil
               :y    nil
               :z    nil
               :type :slime}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance (:sight (rj.cfg/entity->stats :slime))}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :slime))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   [{:type :poison
                                   :duration 3
                                   :value 2
                                   :apply-fn rj.stef/apply-poison}]
               :is-valid-target? #{:player}}]
   [:destructible {:hp         (:hp  (rj.cfg/entity->stats :slime))
                   :max-hp     (:hp  (rj.cfg/entity->stats :slime))
                   :def        (:def (rj.cfg/entity->stats :slime))
                   :can-retaliate? false
                   :on-death-fn    nil
                   :take-damage-fn rj.d/take-damage
                   :status-effects []}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :slime))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the slime")}]])
