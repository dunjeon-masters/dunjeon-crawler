(ns rouje-like.spider
  (:require [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.tickable :as rj.t]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.status-effects :as rj.stef]))

(defentity spider
  [{:keys [system z]}]
  [[:spider {}]
   [:position {:x    (:x tile)
               :y    (:y tile)
               :z    (:z tile)
               :type :spider}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance 3}]
   [:attacker {:atk              (:atk (:spider rj.cfg/entity->stats))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   [{:type :poison
                                   :duration 5
                                   :value 1
                                   :e-from e-this
                                   :apply-fn rj.stef/apply-poison}]
               :is-valid-target? #{:player}}]
   [:destructible {:hp     (:hp  (:spider rj.cfg/entity->stats))
                   :max-hp (:hp  (:spider rj.cfg/entity->stats))
                   :def    (:def (:spider rj.cfg/entity->stats))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn    nil
                   :take-damage-fn rj.d/take-damage}]
   [:killable {:experience (:exp (:spider rj.cfg/entity->stats))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the spider")}]])
