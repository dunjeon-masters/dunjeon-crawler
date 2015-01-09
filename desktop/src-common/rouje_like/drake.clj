(ns rouje-like.drake
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.config :as rj.cfg]
            [rouje-like.status-effects :as rj.stef]))

(defentity drake
  [[:drake {}]
   [:position {:x    nil
               :y    nil
               :z    nil
               :type :drake}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance (:sight (rj.cfg/entity->stats :drake))}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :drake))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   (rj.cfg/mob->stefs :drake)
               :is-valid-target? #{:player}}]
   [:destructible {:hp         (:hp  (rj.cfg/entity->stats :drake))
                   :max-hp     (:hp  (rj.cfg/entity->stats :drake))
                   :def        (:def (rj.cfg/entity->stats :drake))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn nil
                   :take-damage-fn rj.d/take-damage}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :drake))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the drake")}]])
