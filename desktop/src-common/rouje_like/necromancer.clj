(ns rouje-like.necromancer
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.tickable :as rj.t]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.spawnable
             :refer [defentity]]))

(defentity necromancer
  [{:keys [system z]}]
  [[:necromancer {}]
   [:position {:x    (:x tile)
               :y    (:y tile)
               :z    (:z tile)
               :type :necromancer}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance 5}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :necromancer))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   [{:type :paralysis
                                   :duration 3
                                   :value 1
                                   :e-from e-this
                                   :apply-fn rj.stef/apply-paralysis}]
               :is-valid-target? #{:player}}]
   [:destructible {:hp         (:hp  (rj.cfg/entity->stats :necromancer))
                   :max-hp     (:hp  (rj.cfg/entity->stats :necromancer))
                   :def        (:def (rj.cfg/entity->stats :necromancer))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn nil
                   :take-damage-fn rj.d/take-damage}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :necromancer))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the necromancer")}]])
