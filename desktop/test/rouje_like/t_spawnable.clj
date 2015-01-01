(ns rouje-like.t-spawnable
  (:use [midje.sweet]
        [rouje-like.spawnable]))

(fact :spawnable
      "defentity expands to correct code"
      (defentity skeleton
        [{:keys [system z]}]
        [[:skeleton {}]])
      =expands-to=>
      (clojure.core/defn add-skeleton
        [{:keys [system z]}]
        (clojure.core/let [tile (rouje-like.spawnable/get-tile system z)
                           entity (rouje-like.spawnable/new-entity)
                           type-e :skeleton]
          (clojure.core/->>
            (rouje-like.entity-wrapper/system<<components
              system entity
              [[:skeleton {}]])
            (rouje-like.spawnable/put-in-world type-e tile entity z)
            (clojure.core/assoc {} :z z :system)))))
