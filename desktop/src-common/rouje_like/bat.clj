(ns rouje-like.bat
  (:require [rouje-like.tickable :as rj.t]
            [rouje-like.spawnable
             :refer [defentity]]
            [rouje-like.destructible :as rj.d]
            [rouje-like.mobile :as rj.m]
            [rouje-like.config :as rj.cfg]))

(defentity bat
  [[:bat {}]
   [:position {:x    nil
               :y    nil
               :z    nil
               :type :bat}]
   [:mobile {:can-move?-fn rj.m/can-move?
             :move-fn      rj.m/move}]
   [:destructible {:hp     (:hp  (:bat rj.cfg/entity->stats))
                   :max-hp (:hp  (:bat rj.cfg/entity->stats))
                   :def    (:def (:bat rj.cfg/entity->stats))
                   :can-retaliate? false
                   :take-damage-fn rj.d/take-damage
                   :on-death-fn    nil
                   :status-effects []}]
   [:tickable {:tick-fn rj.t/process-input-tick
               :extra-tick-fn nil
               :pri 0}]
   [:broadcaster {:name-fn (constantly "the bat")}]])
