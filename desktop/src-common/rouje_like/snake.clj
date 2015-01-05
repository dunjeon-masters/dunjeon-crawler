(ns rouje-like.snake
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.config :as rj.cfg]))

(defentity snake
  [[:snake {}]
   [:position {:x    (:x tile)
               :y    (:y tile)
               :z    (:z tile)
               :type :snake}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance (:sight (rj.cfg/entity->stats :snake))}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :snake))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   [{:type :poison
                                   :duration 4
                                   :value 2
                                   :e-from e-this
                                   :apply-fn rj.stef/apply-poison}]
               :is-valid-target? (partial #{:player})}]
   [:destructible {:hp         (:hp (rj.cfg/entity->stats :snake))
                   :max-hp     (:hp (rj.cfg/entity->stats :snake))
                   :def        (:def (rj.cfg/entity->stats :snake))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn    nil
                   :take-damage-fn rj.d/take-damage}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :snake))}]
   [:energy {:energy 2
             :default-energy 2}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the snake")}]])
