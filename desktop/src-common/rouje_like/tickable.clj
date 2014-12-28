(ns rouje-like.tickable
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u
             :refer [?]]
            [rouje-like.destructible :as rj.d]
            [rouje-like.components :as rj.c
             :refer [can-move? move
                     can-attack? attack
                     ->2DPoint]]))

(defn get-target-tile
  [e-this e-target e-world system]
  (let [{:keys [levels]} (rj.e/get-c-on-e system e-world :world)
        {:keys [x y z]
         target-type :type} (rj.e/get-c-on-e system e-target :position)
        level (nth levels z)
        {:keys [distance]
         :or {distance 0}} (rj.e/get-c-on-e system e-this :sight)
        this-pos (->2DPoint (rj.e/get-c-on-e system e-this :position))
        is-target-within-range? (seq (rj.u/get-neighbors-of-type-within
                                       level this-pos [target-type]
                                       #(<= % distance)))
        neighbor-tiles (rj.u/get-neighbors level this-pos)]
    (if (and (rj.u/can-see? level distance this-pos [x y])
             is-target-within-range?)
      (rj.u/get-closest-tile-to level this-pos (first is-target-within-range?))
      (when (seq neighbor-tiles)
        (rand-nth neighbor-tiles)))))

(defn process-input-tick
  [{:keys [target-tile-fn
           extra-tick-fn
           target-e]
    :or {target-tile-fn get-target-tile
         target-e :player}}
   e-this system]
  (if-let [_ (rj.e/get-c-on-e system e-this :tickable)]
    (let [c-energy (rj.e/get-c-on-e system e-this :energy)
          {:keys [energy] :or {energy 1}} c-energy]
      (loop [system system
             energy energy]
        (if (pos? energy)
          (recur (let [e-target (first (rj.e/all-e-with-c system target-e))
                       e-world (first (rj.e/all-e-with-c system :world))

                       target-tile (target-tile-fn e-this e-target e-world system)
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
                       (if (and extra-tick-fn
                                (rj.e/get-c-on-e system e-this :tickable))
                         (extra-tick-fn e-this e-target e-world system)
                         system)
                       (if-let [c-energy (rj.e/get-c-on-e system e-this :energy)]
                         (rj.e/upd-c system e-this :energy
                                     #(update-in % [:energy] dec))
                         system))
                     system))
                 (dec energy))
          system)))
    system))
