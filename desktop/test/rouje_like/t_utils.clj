(ns rouje-like.t-utils
  (:use [midje.sweet]
        [rouje-like.test-utils])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(def level (rj.w/generate-random-level
             {:width 3 :height 3} 1 :merchant))
(def wall-e (rj.c/map->Entity {:type :wall}))
(def level+wall (update-in level [0 1 :entities]
                           conj wall-e))

(fact "taxicab-dist"
      (taxicab-dist [0 0] [1 2]) => 3)

(fact "tile->top-entity"
      (let [entities [(rj.c/map->Entity {:type :player})
                      (rj.c/map->Entity {:type :floor})]
            tile (rj.c/map->Tile {:entities entities
                                  :x 1 :y 2 :z 3})]
        (tile->top-entity tile)) => (contains {:type :player}))

(fact "points->line"
      (points->line [0 0] [3 5]) => [[0 0] [0 1]
                                     [1 2] [2 3]
                                     [2 4] [3 5]])

(fact "can-see?"
      (can-see? level+wall 3 [0 0] [2 1]) => true
      (can-see? level+wall 3 [0 0] [4 0]) => false
      (can-see? level+wall 3 [0 0] [1 2]) => false)

(fact "coords+offset"
      (coords+offset [0 0] [1 3]) => [1 3]
      (coords+offset [-1 3] [4 -2]) => [3 1])

(fact "get-neighbors-coords"
      (get-neighbors-coords [1 1]) => [[1 0] [1 2]
                                       [2 1] [0 1]])

(facts "get-neighbors"
       (get-neighbors level [1 1])
       => (just [(contains {:x 1 :y 0 :z 1})
                 (contains {:x 1 :y 2 :z 1})
                 (contains {:x 2 :y 1 :z 1})
                 (contains {:x 0 :y 1 :z 1})]))

(fact "get-neighbors-of-type"
      (first (filter #(#{:wall} (:type %))
                     (:entities (first (get-neighbors-of-type level+wall [0 0] [:wall])))))
      => (contains {:type :wall}))

(fact "radial-distance"
      (radial-distance [0 0] [2 2]) => 2
      (radial-distance [4 4] [5 5]) => 1)

(fact "get-entities-radially"
      (get-entities-radially level+wall [0 0]
                             #(<= % 1))
      => (just [(contains {:x 0 :y 0})
                (contains {:x 0 :y 1})
                (contains {:x 1 :y 0})
                (contains {:x 1 :y 1})]))

(fact "get-neighbors-of-type-within"
      (first
        (filter #(#{:wall} (:type %))
                (:entities (first (get-neighbors-of-type-within level+wall [0 0]
                                                                [:wall] #(<= % 1))))))
      => (contains {:type :wall}))

(fact "not-any-radially-of-type"
      (not-any-radially-of-type level [0 0]
                                #(<= % 1) [:wall])
      => true)

(fact "ring-coords"
      (ring-coords [0 0] 3) => (just
                                 [-3 -3] [-3 -2] [-3 -1] [-3 0]
                                 [-3 1]  [-3 2]  [-3 3]  [-2 -3]
                                 [-2 3]  [-1 -3] [-1 3]  [0 -3]
                                 [0 3]   [1 -3]  [1 3]   [2 -3]
                                 [2 3]   [3 -3]  [3 -2]  [3 -1]
                                 [3 0]   [3 1]   [3 2]   [3 3]))

(fact "get-ring-around"
      (get-ring-around level [0 0] 2)
      => (every-checker
           #(= 16 (count %))
           #(= 5 (count
                   (filter (fn [tile]
                             (= :dune (:type (tile->top-entity tile))))
                           %)))))

(fact "rand-rng"
      (* 1/10000 (apply + (take 10000 (repeatedly #(rand-rng 1 10)))))
      => (roughly 5 1))

(fact "update-in-world"
      (let [system (start)
            e-world (first (rj.e/all-e-with-c system :world))
            e-player (first (rj.e/all-e-with-c system :player))
            system (update-in-world system e-world [1 3 3];[z x y]
                                    (fn [es]
                                      [(rj.c/strict-map->Entity
                                         {:id nil
                                          :extra nil
                                          :type :fact})]))
            c-world (rj.e/get-c-on-e system e-world :world)
            levels (:levels c-world)]
        (entities-at-pos levels [1 3 3]))
      => (fn [es]
           (= (:type (first es)) :fact)))

(fact "change-type"
      (let [system (start)
            e-player (first (rj.e/all-e-with-c system :player))
            system (change-type system e-player :player :reyalp)
            c-position (rj.e/get-c-on-e system e-player :position)
            e-world (first (rj.e/all-e-with-c system :world))
            levels (:levels (rj.e/get-c-on-e system e-world :world))
            entities (entities-at-pos levels (->3DPoint c-position))]
        {:c-position c-position
         :entities  entities})
      => (fn [{:keys [c-position entities]}]
           (and (#{:reyalp} (:type c-position))
                  (seq
                    (filter #(= :reyalp (:type %))
                            entities)))))
