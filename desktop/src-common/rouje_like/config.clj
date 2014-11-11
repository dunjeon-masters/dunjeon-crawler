(ns rouje-like.config
  (:require [clojure.set :refer [union]]))

;; WORLD CONFIG
(def block-size 36)
(def padding-sizes {:top   3
                    :btm   2
                    :left  1
                    :right 1})
(def view-port-sizes [20 20])
(def world-sizes {:width  20
                  :height 20})

;; TILE TYPES
(def <floors>
  #{:dune :floor :forest-floor})

(def <walls>
  #{:wall :tree :maze-wall})

(def <items>
  #{:torch :gold :health-potion :equipment})

(def <empty>
  (union <floors> <items>))

(def <sight-blockers>
  (union <walls> #{:lichen}))

(def <valid-move-targets>
  (union <empty> #{:portal}))

(def <valid-mob-targets>
  (union <empty> #{:player}))

(def wall->stats
  {:wall      {:hp 2}
   :tree      {:hp 1}
   :maze-wall {:hp 100}})

;; PLAYER CONFIG
(def player-stats
  {:max-hp 100
   :atk 4
   :def 1})

(def class->stats {:rogue   {}
                   :warrior {}
                   :mage    {}})

(def race->stats {:human {:max-hp 10  :atk 1}
                  :orc   {:max-hp 20  :atk 2}
                  :elf   {:max-hp -5  :atk 0}})

(def stat->comp {:max-hp :destructible
                 :atk :attacker
                 :def :destructible})

(def stat->pointinc {:max-hp 5
                     :atk 1
                     :def 1})

(def level-exp
  {:exp 1})

;; EQUIPMENT CONFIG
(def weapons
  [[:sword  {:atk 3}]
   [:mace   {:atk 2}]
   [:axe    {:atk 3}]
   [:flail  {:atk 2}]
   [:dagger {:atk 1}]])

(def weapon-qualities
  [[:quick  {:atk  1}]
   [:giant  {:atk  2}]
   [:great  {:atk  2}]
   [:tiny   {:atk  1}]
   [:dull   {:atk -1}]
   [:dented {:atk -2}]])

(def weapon-effects
  [[:bloodletting]
   [:pain]
   [:poison]
   [:paralysis]
   [:power]
   [:death]
   [nil]])

(def armors
  [[:chestplate {:def 1 :max-hp 1}]
   [:chainmail  {:max-hp 3}]
   [:tunic      {:max-hp 1}]])

;; CREATURE CONFIG
(def bat-stats
  {:hp  2
   :def 0})

(def lichen-stats
  {:hp  4
   :atk 0
   :def 1})

(def skeleton-stats
  {:hp  8
   :def 1
   :atk 3
   :exp 1})

(def potion-stats
  {:health 5})

