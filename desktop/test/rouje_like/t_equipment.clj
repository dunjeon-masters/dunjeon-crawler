(ns rouje-like.t-equipment
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.equipment])
  (:require [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.core :as rj.core]))

(def arm {:type :armor, :stats {:max-hp 3 :def 2}, :name :chainmail})

(def wpn {:type :weapon, :effect :bloodletting, :name :axe, :stats {:atk 5}, :quality :great})

;; ========= GENERATE-RANDOM-WEAPON =========
(let [wpn (generate-random-weapon)]
  (fact "generate-random-weapon generates a weapon"
        (:type wpn) => :weapon))

;; ========= GENERATE-RANDOM-ARMOR =========
(let [arm (generate-random-armor)]
  (fact "generate-random-armor generates a piece of armor"
        (:type arm) => :armor))

;; ========= GENERATE-RANDOM-EQUIPMENT =========
(let [eq (generate-random-equipment)]
  (fact "generate-random-equipment generates a weapon or piece of armor"
        (:type eq) => #(or (= % :armor)
                           (= % :weapon))))

;; ========= EQUIPMENT-NAME =========
(facts "equipment-name"
       (fact "equipment-name returns a string"
             (equipment-name wpn) => string?)
       (fact "equipment-name returns the name of the equipment piece"
             (equipment-name wpn) => "great axe of bloodletting"
             (equipment-name arm) => "chainmail"))

;; ========= EQUIPMENT-VALUE =========
(facts "equipment-value"
       (fact "equipment-value returns a number"
             (equipment-value wpn) => number?)
       (fact "equipment-value returns the aggregate value of equipment's stats"
             (equipment-value wpn) => 5
             (equipment-value arm) => 5))

;; ========= UPDATE-STAT =========
(let [system (start)
      e-player (first (rj.e/all-e-with-c system :player))
      c-atk (rj.e/get-c-on-e system e-player :attacker)
      old-atk (:atk c-atk)]
  (as-> system system
        (update-stat system e-player :atk 100)
        (let [c-atk (rj.e/get-c-on-e system e-player :attacker)
              new-atk (:atk c-atk)]
          (fact "update-stat updates a stat on an entity"
                new-atk => (+ old-atk 100)))))

;; ========= UPDATE-STATS =========
(let [system (start)
      e-player (first (rj.e/all-e-with-c system :player))

      c-atk (rj.e/get-c-on-e system e-player :attacker)
      old-atk (:atk c-atk)

      c-dest (rj.e/get-c-on-e system e-player :destructible)
      old-max-hp (:max-hp c-dest)]
  (as-> system system
        (update-stats system e-player {:atk 100 :max-hp 100})
        (let [c-atk (rj.e/get-c-on-e system e-player :attacker)
              new-atk (:atk c-atk)

              c-dest (rj.e/get-c-on-e system e-player :destructible)
              new-max-hp (:max-hp c-dest)]
          (fact "update-stats updates multiple stats on an entity"
                new-atk    => (+ old-atk 100)
                new-max-hp => (+ old-max-hp 100)))))

;; ========= UPDATE-VALUES =========
(fact "update-values"
      (update-values {:one 1 :two 2} inc) => {:one 2 :two 3})

;; ========= GET-STAT-MAP =========
(fact "get-stat-map returns a {:duration n :value n :type keyword}"
      (get-stat-map :fire) => (just {:duration number? :value number? :type keyword?}))

;; ========= ADD-EFFECT =========
(let [system (start)
      e-player (first (rj.e/all-e-with-c system :player))

      c-atk-init (rj.e/get-c-on-e system e-player :attacker)
      init-effects (:status-effects c-atk-init)

      system (add-effect system e-player :paralysis)
      c-atk-new (rj.e/get-c-on-e system e-player :attacker)
      new-effects (:status-effects c-atk-new)]
  (facts "add-effect"
         (fact "a player initially has no status effects on them"
               init-effects => empty?)
         (fact "add-effect applies an effect to an entity"
               new-effects => truthy)))

;; ========= REMOVE-EFFECTS =========
(let [system (start)
      e-player (first (rj.e/all-e-with-c system :player))

      system (add-effect system e-player :paralysis)
      c-atk-init (rj.e/get-c-on-e system e-player :attacker)
      init-effects (:status-effects c-atk-init)

      system (remove-effects system e-player)
      c-atk-new (rj.e/get-c-on-e system e-player :attacker)
      new-effects (:status-effects c-atk-new)]
  (facts "remove-effects"
         (fact "a player initially has a status effect on them"
               init-effects => truthy)
         (fact "remove-effects removes all effects from an entity"
               new-effects => empty?)))

;; ========= SWITCH-EQUIPMENT =========
(let [system (start)
      e-player (first (rj.e/all-e-with-c system :player))

      c-atk (rj.e/get-c-on-e system e-player :attacker)
      old-atk (:atk c-atk)

      system (switch-equipment system e-player wpn)]
  (facts "switch-equipment"
         (fact "switch-equipment equips the new equipment on the player"
               (let [c-eq (rj.e/get-c-on-e system e-player :equipment)
                     eq (:weapon c-eq)]
                 eq => wpn))
         (fact "switch-equipment updates the stats on player from the equipment"
               (let [c-atk (rj.e/get-c-on-e system e-player :attacker)
                     new-atk (:atk c-atk)]
                 new-atk => (+ old-atk (:atk (:stats wpn)))))))
