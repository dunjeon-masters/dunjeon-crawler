(ns rouje-like.skeleton
  (:require [rouje-like.tickable :as rj.t]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.spawnable
             :refer [defentity]]))

#_(use 'rouje-like.skeleton :reload)

(defentity skeleton
  [[:skeleton     {}]
   [:position     {:x    nil
                   :y    nil
                   :z    nil
                   :type :skeleton}]
   [:mobile       {:can-move?-fn rj.m/can-move?
                   :move-fn rj.m/move}]
   [:sight        {:distance (:sight (rj.cfg/entity->stats :skeleton))}]
   [:attacker     {:atk              (:atk (rj.cfg/entity->stats :skeleton))
                   :can-attack?-fn   rj.atk/can-attack?
                   :attack-fn        rj.atk/attack
                   :status-effects   []
                   :is-valid-target? #{:player}}]
   [:destructible {:hp             (:hp  (rj.cfg/entity->stats :skeleton))
                   :max-hp         (:hp  (rj.cfg/entity->stats :skeleton))
                   :def            (:def (rj.cfg/entity->stats :skeleton))
                   :can-retaliate? false
                   :take-damage-fn rj.d/take-damage
                   :on-death-fn    nil
                   :status-effects []}]
   [:killable     {:experience (:exp (rj.cfg/entity->stats :skeleton))}]
   [:energy       {:energy 1
                   :default-energy 1}]
   [:tickable     {:tick-fn rj.t/process-input-tick
                   :extra-tick-fn nil
                   :pri 0}]
   [:broadcaster  {:name-fn (constantly "the skeleton")}]])
