(ns rouje-like.skeleton
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.tickable :as rj.t]
            [rouje-like.components :as rj.c
             :refer [can-move? move
                     can-attack? attack
                     ->3DPoint ->2DPoint]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]
            [rouje-like.spawnable :as rj.spawn]

            [clojure.set :refer [union]]))

#_(use 'rouje-like.skeleton :reload)

(defn add-skeleton
  [{:keys [system z]}]
  (let [tile (rj.spawn/get-tile system z)
        e-skeleton (rj.spawn/new-entity)
        type-e :skeleton]
    (->>
      (rj.e/system<<components
        system e-skeleton
        [[:skeleton     {}]
         [:position     {:x    (:x tile)
                         :y    (:y tile)
                         :z    (:z tile)
                         :type type-e}]
         [:mobile       {:can-move?-fn (fn [c-mobile e-this t-tile system]
                                         (rj.m/can-move? c-mobile e-this t-tile system))
                         :move-fn rj.m/move}]
         [:sight        {:distance 4}]
         [:attacker     {:atk              (:atk (rj.cfg/entity->stats type-e))
                         :can-attack?-fn   rj.atk/can-attack?
                         :attack-fn        rj.atk/attack
                         :status-effects   []
                         :is-valid-target? (partial #{:player})}]
         [:destructible {:hp             (:hp  (rj.cfg/entity->stats type-e))
                         :max-hp         (:hp  (rj.cfg/entity->stats type-e))
                         :def            (:def (rj.cfg/entity->stats type-e))
                         :can-retaliate? false
                         :take-damage-fn rj.d/take-damage
                         :on-death-fn    nil
                         :status-effects []}]
         [:killable     {:experience (:exp (rj.cfg/entity->stats type-e))}]
         [:energy       {:energy 1
                         :default-energy 1}]
         [:tickable     {:tick-fn rj.t/process-input-tick
                         :pri 0}]
         [:broadcaster  {:name-fn (constantly "the skeleton")}]])
      (rj.spawn/put-in-world type-e tile e-skeleton z)
      (assoc {} :z z :system))))
