(ns rouje-like.t-merchant
  (:use [midje.sweet]
        [rouje-like.test-utils])
  (:require [brute.entity :as br.e]
            [rouje-like.world :as rj.w]
            [rouje-like.components :as rj.c]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.config :as rj.cfg]
            [rouje-like.items :as rj.i]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.core :as rj.core]))

(let [system (start)
      e-world (first (rj.e/all-e-with-c system :world))
      c-world (rj.e/get-c-on-e system e-world :world)
      levels (:levels c-world)
      merch-level (nth levels 0)
      merch-pos rj.cfg/merchant-pos
      merch-portal-pos rj.cfg/merchant-portal-pos
      merch-player-pos rj.cfg/merchant-player-pos]
  (fact "merchant-tile"
        [(:x merch-pos) (:y merch-pos)]
        => [10 12])
  (fact "merchant-portal-tile"
        [(:x merch-portal-pos) (:y merch-portal-pos)]
        => [10 14])
  (fact "merchant-player-tile"
        [(:x merch-player-pos) (:y merch-player-pos)]
        => [10 8])
  (fact "add-merchant"
        (:type (rj.u/tile->top-entity
                 (get-in merch-level [(:x merch-pos) (:y merch-pos)])))
        => :merchant))

