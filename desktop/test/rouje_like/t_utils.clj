(ns rouje-like.t-utils
  (:use midje.sweet)
  (:require [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c]
            [rouje-like.world :as rj.w]))

(fact "taxicab-dist"
      (taxicab-dist [0 0] [1 1]) => 2)

(fact "tile->top-entity"
      (let [entities [(rj.c/map->Entity {:type :player})
                      (rj.c/map->Entity {:type :floor})]
            tile (rj.c/map->Tile {:entities entities})]
        (tile->top-entity tile)) => {:id nil,
                                     :type :player})

(fact "points->line"
      (points->line [0 0] [2 3])
      => [[0 0] [0 1] [1 1] [1 2] [2 3]])
(let [level (rj.w/generate-random-level
              {:width 3 :height 3} 1 :merchant)
      wall-e (rj.c/map->Entity {:type :wall})
      level2 (update-in level [0 1 :entities]
                        conj wall-e)]
  (fact "can-see?"
        (can-see? level 3 [0 0] [1 2]) => true
        (can-see? level2 3 [0 0] [1 2]) => false))

(fact "coords+offset"
      (coords+offset [0 0] [1 3]) => [1 3])

(fact "get-neighbors-coords"
      (get-neighbors-coords [1 1]) => [[1 0] [1 2] [2 1] [0 1]])

(fact "get-neighbors")

(fact "get-neighbors-of-type")

(fact "radial-distance"
      (radial-distance [0 0] [2 2]) => 2
      (radial-distance [4 4] [5 5]) => 1)
