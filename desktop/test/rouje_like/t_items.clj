(ns rouje-like.t-items
  (:use [midje.sweet]
        [rouje-like.items])
  (:require [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.world :as rj.w]))

(defn get-system []
  (with-open [w (clojure.java.io/writer "NUL")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(def level (rj.w/generate-random-level
             {:width 3 :height 3} 1 :merchant))
(def wall-e (rj.c/map->Entity {:type :wall}))
(def level+wall (update-in level [0 1 :entities]
                           conj wall-e))

(let [system get-system
      e-world (first (rj.e/all-e-with-c system :world))
      c-world (rj.e/get-c-on-e system e-world :world)
      levels (:levels c-world)
      z 1
      world (nth levels z)]

  (fact "remove-item")

  (fact "pickup-item")

  (fact "use-hp-potion")

  (fact "use-mp-potion")

  (fact "item>>world")

  (fact "only-floor?")

  (fact "item>>entities")

  (fact "add-health-potion")

  (fact "add-magic-potion")

  (fact "add-torch")

  (fact "add-gold")

  (fact "add-purchasable")

  (fact "add-equipment"))
