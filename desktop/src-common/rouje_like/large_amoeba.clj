(ns rouje-like.large-amoeba
  (:require [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.tickable :as rj.t]
            [rouje-like.config :as rj.cfg]))

(defentity large-amoeba
  [[:large-amoeba {}]
   [:position {:x    nil
               :y    nil
               :z    nil
               :type :large-amoeba}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:sight {:distance (:sight (rj.cfg/entity->stats :large-amoeba))}]
   [:attacker {:atk              (:atk (rj.cfg/entity->stats :large-amoeba))
               :can-attack?-fn   rj.atk/can-attack?
               :attack-fn        rj.atk/attack
               :status-effects   []
               :is-valid-target? #{:player}}]
   [:destructible {:hp     (:hp  (rj.cfg/entity->stats :large-amoeba))
                   :max-hp (:hp  (rj.cfg/entity->stats :large-amoeba))
                   :def    (:def (rj.cfg/entity->stats :large-amoeba))
                   :can-retaliate? false
                   :status-effects []
                   :on-death-fn    nil
                   :take-damage-fn rj.d/take-damage}]
   [:killable {:experience (:exp (rj.cfg/entity->stats :large-amoeba))}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the large-amoeba")}]])
