(ns rouje-like.t-spawnable
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.spawnable])
  (:require [rouje-like.entity-wrapper :as rj.e]))

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

(fact "give-position"
      true =future=> false)

(fact "give-stef-e-from"
      false =future=> true)

(defentity skeleton
  [[:skeleton {}]
   [:position {:x nil
               :y nil
               :z nil
               :type :skeleton}]
   [:destructible {:status-effects [{:should :be-filled}]
                   :def nil
                   :hp nil
                   :max-hp nil
                   :can-retaliate? nil
                   :on-death-fn nil
                   :take-damage-fn nil}]])

(fact "defentity expands to correct code"
      (add-skeleton {:system system :z 1})
      => (fn [{:keys [system z]}]
           (let [e-skeleton (first (rj.e/all-e-with-c system :skeleton))]
             (and (let [{:keys [x y z]} (rj.e/get-c-on-e system e-skeleton :position)]
                    (and x y z))
                  (let [{:keys [status-effects]} (rj.e/get-c-on-e system e-skeleton :destructible)]
                    (:e-from (first status-effects)))))))
