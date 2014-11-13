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
  #{:open-door :dune :floor :forest-floor})

(def <walls>
  #{:temple-wall :door :wall :tree :maze-wall})

(def <indestructible-walls>
  #{:temple-wall :maze-wall})

(def <items>
  #{:torch :gold :health-potion})

(def <empty>
  (union <floors> <items>))

(def <sight-blockers>
  (union <walls> #{:lichen}))

(def <valid-move-targets>
  (union <empty> #{:portal}))

(def <valid-mob-targets>
  (union <empty> #{:player}))

(def wall->stats
  {:wall        {:hp 2}
   :tree        {:hp 1}
   :maze-wall   {:hp 100}
   :temple-wall {:hp 100}})

(def trap->stats
  {:arrow-trap {:hp 1}})

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

(def trap-stats
  {:atk 1})

(def trap-types
  [:arrow])

(def potion-stats
  {:health 5})

