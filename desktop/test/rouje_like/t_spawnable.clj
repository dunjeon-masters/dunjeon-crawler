(ns rouje-like.t-spawnable
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.spawnable]))

(def system (start))

(facts "get-tile"
       (fact "returns a tile"
             (type (get-tile system 0))
             => rouje_like.components.Tile)
       (fact "should contain a dune if on lvl 0 (ie: merch-level)"
             (:entities (get-tile system 0))
             => (contains {:extra nil
                           :id    nil
                           :type  :dune})))

(fact "new-entity"
      (type (new-entity))
      => java.util.UUID)

(fact "put-in-world"
      (put-in-world :type-e "tile" "entity" 0 system)
      =future=> nil)

(fact :spawnable
      "defentity expands to correct code"
      (defentity skeleton
        [[:skeleton {}]])
      =expands-to=>
      (clojure.core/defn add-skeleton
        ([{:keys [system z]}]
         (clojure.core/let [tile (rouje-like.spawnable/get-tile system z)]
           (add-skeleton system tile)))
        ([system tile]
         (clojure.core/let [e-this (rouje-like.spawnable/new-entity)
                            type-e :skeleton
                            z      (:z tile)]
           (clojure.core/->>
             (rouje-like.entity-wrapper/system<<components
               system e-this
               [[:skeleton {}]])
             (rouje-like.spawnable/put-in-world type-e tile e-this z)
             (clojure.core/assoc {} :z z :system))))))
