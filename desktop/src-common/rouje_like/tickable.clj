(ns rouje-like.tickable
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.destructible :as rj.d]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]))

(defn process-input-tick
  [{:keys [target-tile-fn
           extra-tick-fn]}
   e-this system]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        e-world (first (rj.e/all-e-with-c system :world))

        target-tile (target-tile-fn e-this e-player e-world system)
        e-target (:id (rj.u/tile->top-entity target-tile))]
    (if target-tile
      (as-> (let [c-mobile (rj.e/get-c-on-e system e-this :mobile)
                  c-mobile (if c-mobile c-mobile
                             (rj.c/map->Mobile
                               {:can-move?-fn (constantly false)}))
                  c-attacker (rj.e/get-c-on-e system e-this :attacker)
                  c-attacker (if c-attacker c-attacker
                             (rj.c/map->Attacker
                               {:can-attack?-fn (constantly false)}))]
              (cond
                (can-move? c-mobile e-this target-tile system)
                (move c-mobile e-this target-tile system)

                (can-attack? c-attacker e-this e-target system)
                (attack c-attacker e-this e-target system)

                :else system)) system
        (if-let [c-destructible (rj.e/get-c-on-e system e-this :destructible)]
          (rj.d/apply-effects system e-this)
          system)
        (if extra-tick-fn
          (extra-tick-fn e-this e-player e-world system)
          system))
      system)))