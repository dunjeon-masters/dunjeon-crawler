(ns rouje-like.merchant
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.config :as rj.cfg]
            [rouje-like.items :as rj.i]
            [rouje-like.utils :as rj.u :refer [?]]))

(defn merchant-tile
  [system]
  "Return the tile the merchant is in."
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-pos rj.cfg/merchant-pos]
    (get-in merch-level [(:x merch-pos) (:y merch-pos)])))

(defn merchant-portal-tile
  [system]
  "Return the tile the exit portal to the merchant level is in."
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-portal-pos rj.cfg/merchant-portal-pos]
    (get-in merch-level [(:x merch-portal-pos) (:y merch-portal-pos)])))

(defn merchant-player-tile
  [system]
  "Return the tile that the player spawns at in the merchant level."
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-player-pos rj.cfg/merchant-player-pos]
    (get-in merch-level [(:x merch-player-pos) (:y merch-player-pos)])))

(defn merchant-item-tiles
  [system]
  "Return a vector of tiles that the merchant's purchasable items are in."
  (let [e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        merch-level (nth levels 0)
        merch-item-pos rj.cfg/merchant-item-pos]
    (map (fn [{x :x y :y}]
           (get-in merch-level [x y]))
         merch-item-pos)))

(defn add-merchant
  [system]
  (let [target-tile (merchant-tile system)
        e-world (first (rj.e/all-e-with-c system :world))
        e-merchant (br.e/create-entity)
        system (rj.u/update-in-world system e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                     (fn [entities]
                                       (vec
                                        (conj
                                         (remove #(#{:wall} (:type %)) entities)
                                         (rj.c/map->Entity {:id   e-merchant
                                                            :type :merchant})))))]
    (rj.e/system<<components
              system e-merchant
              [[:merchant {}]
               [:item {:pickup-fn rj.i/pickup-item}]
               [:inspectable {:msg "the merchant says: bring your junk to me for gold"}]])))

(defn init-merchant
  [system z]
  (add-merchant system))
