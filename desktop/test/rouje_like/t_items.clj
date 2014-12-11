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

(let [system (get-system)
      e-world (first (rj.e/all-e-with-c system :world))
      {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
      world (nth levels 1)]

  (fact "remove-item")

  (fact "pickup-item")

  (fact "use-hp-potion")

  (fact "use-mp-potion")

  (fact "item>>world")

  (fact "only-floor?")

  (fact "item>>entities")

  (fact "add-health-potion"
        (let [system (:system
                       (add-magic-potion {:system system
                                   :z 0}))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :magic-potion %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:magic-potion}
                        (:type %))))

  (fact "add-magic-potion"
        (let [system (:system
                       (add-magic-potion {:system system
                                   :z 0}))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :magic-potion %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:magic-potion}
                        (:type %))))

  (fact "add-torch"
        (let [system (:system
                       (add-torch {:system system
                                   :z 0}))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :torch %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:torch}
                        (:type %))))

  (fact "add-gold"
        (let [system (:system
                       (add-gold {:system system
                                  :z 0}))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :gold %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:gold}
                        (:type %))))

  (fact "add-purchasable"
        (let [e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)
              target-tile (rand-nth (flatten m-level))
              system (:system
                       (add-purchasable system target-tile))

              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :purchasable %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:purchasable}
                        (:type %))))

  (fact "add-equipment"
        (let [system (:system
                       (add-equipment {:system system
                                       :z 0}))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :equipment %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:equipment}
                        (:type %)))))
