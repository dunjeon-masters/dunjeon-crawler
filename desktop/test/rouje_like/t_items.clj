(ns rouje-like.t-items
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.items])
  (:require [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.world :as rj.w]))

(let [system (start)
      e-world (first (rj.e/all-e-with-c system :world))
      {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
      level (nth levels 1)
      e-player (first (rj.e/all-e-with-c system :player))]

  (fact "remove-item"
        (let [torch-tile (first
                           (filter #(= :torch (:type (rj.u/tile->top-entity %)))
                                   (flatten level)))
              torch-pos (rj.c/->3DPoint torch-tile)
              system (remove-item system torch-pos :torch)
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)]
          (:entities
            (get-in levels torch-pos nil)))
        =not=> (has every? (contains {:type :torch})))

  (fact "pickup-item"
        )

  (facts "use-hp-potion"
         (fact "use-hp-potion: no potions"
               (let [system (use-hp-potion system e-player)
                     e-relay (first (rj.e/all-e-with-c system :relay))
                     {:keys [static]} (rj.e/get-c-on-e system e-relay :relay)]
                 static)
               => (contains
                    #(= "You do not have any health potions to drink"
                        (:message %))))
         (fact "use-hp-potion: have potions, need health"
               (let [system (rj.e/upd-c system e-player :destructible
                                        #(assoc % :hp 10))
                     system (rj.e/upd-c system e-player :inventory
                                        #(assoc % :hp-potion 1))
                     system (use-hp-potion system e-player)
                     {:keys [hp]} (rj.e/get-c-on-e system e-player :destructible)]
                 hp) => 15)
         (fact "use-hp-potion: have potions, dont need health"
               (let [system (rj.e/upd-c system e-player :inventory
                                        #(assoc % :hp-potion 1))
                     system (use-hp-potion system e-player)
                     {:keys [hp]} (rj.e/get-c-on-e system e-player :destructible)]
                 hp) => (:max-hp (rj.e/get-c-on-e system e-player :destructible))))

  (facts "use-mp-potion"
         (fact "use-mp-potion: no potions"
               (let [system (use-mp-potion system e-player)
                     e-relay (first (rj.e/all-e-with-c system :relay))
                     {:keys [static]} (rj.e/get-c-on-e system e-relay :relay)]
                 static)
               => (contains
                    #(= "You do not have any mana potions to drink"
                        (:message %))))
         (fact "use-mp-potion: have potions, need mana"
               (let [system (rj.e/upd-c system e-player :magic
                                        #(assoc % :mp 0))
                     system (rj.e/upd-c system e-player :inventory
                                        #(assoc % :mp-potion 1))
                     system (use-mp-potion system e-player)
                     {:keys [mp]} (rj.e/get-c-on-e system e-player :magic)]
                 mp) => 3)
         (fact "use-mp-potion: have potions, dont need mana"
               (let [system (rj.e/upd-c system e-player :inventory
                                        #(assoc % :mp-potion 1))
                     system (use-mp-potion system e-player)
                     {:keys [mp]} (rj.e/get-c-on-e system e-player :magic)]
                 mp) => (:max-mp (rj.e/get-c-on-e system e-player :magic))))

  (fact "item>>world"
        (let [system (item>>world system (fn [world [x y]]
                                           (let [tile (get-in level [x y])]
                                             (only-floor? tile)))
                                  0 (fn [entities]
                                      (item>>entities entities :e-torch :torch)))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)
              target-tile (first (flatten m-level))]
          (:entities (first (filter (fn [{:keys [entities]}]
                                      (seq (filter #(= :torch %)
                                                   (map :type entities))))
                                    (flatten m-level)))))
        => (contains #(#{:torch}
                        (:type %))))

  (fact "only-floor?"
        (let [e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)
              target-tile (first (flatten m-level))]
          (only-floor? target-tile)) => truthy)

  (fact "item>>entities"
        (let [e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)
              {:keys [entities]} (rand-nth (flatten m-level))]
          (item>>entities entities :e-id :e-type))
        => (contains #(#{:e-id}
                        (:id %))))

  (fact "add-health-potion"
        (let [system (:system
                       (add-health-potion {:system system
                                           :z 0}))
              e-world (first (rj.e/all-e-with-c system :world))
              {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
              m-level (nth levels 0)]
          (:entities
            (first
              (filter (fn [{:keys [entities]}]
                        (seq (filter #(= :health-potion %)
                                     (map :type entities))))
                      (flatten m-level)))))
        => (contains #(#{:health-potion}
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
