(ns rouje-like.t-spawnable
  (:use [midje.sweet]
        [rouje-like.spawnable]))

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
