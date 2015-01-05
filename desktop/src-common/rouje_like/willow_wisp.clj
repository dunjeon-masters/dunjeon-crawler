(ns rouje-like.willow-wisp
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.status-effects :as rj.stef]))

(defentity willow-wisp
  [[:willow-wisp {}]
   [:position {:x    (:x tile)
               :y    (:y tile)
               :z    (:z tile)
               :type :willow-wisp}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance (:sight (rj.cfg/entity->stats :willow-wisp))}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :willow-wisp))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   [{:type :burn
                                   :duration 2
                                   :value 1
                                   :e-from e-this
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
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the will-o-wisp")}]])
