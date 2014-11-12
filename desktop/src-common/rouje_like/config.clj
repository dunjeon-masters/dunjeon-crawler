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

(def snake-stats
  {:hp  2
   :def 1
   :atk 2
   :exp 1})

(def troll-stats
  {:hp  8
   :def 2
   :atk 4
   :exp 4})

(def mimic-stats
  {:hp 5
   :def 2
   :atk 3
   :exp 3})

(def spider-stats
  {:hp 2
   :def 0
   :atk 1
   :exp 1})

(def slime-stats
  {:hp 2
   :def 0
   :atk 1
   :exp 1})

(def drake-stats
  {:hp 6
   :def 3
   :atk 4
   :exp 5})

(def necro-stats
  {:hp 6
   :def 3
   :atk 4
   :exp 5})

(def giant_amoeba-stats
  {:hp 4
   :def 1
   :atk 2
   :exp 0})

(def large_amoeba-stats
  {:hp 2
   :def 1
   :atk 1
   :exp 2})

(def potion-stats
  {:health 5})

;; WORLD CONFIG
(def ^:private init-wall% 45)
(def ^:private init-torch% 2)
(def ^:private init-gold% 5)
(def ^:private init-health-potion% 2)
(def ^:private init-lichen% 1)
(def ^:private init-bat% 1)
(def ^:private init-skeleton% 0.1)
(def ^:private init-snake% 0.3)
(def ^:private init-troll% 0.1)
(def ^:private init-mimic% 0.1)
(def ^:private init-spider% 0.5)
(def ^:private init-slime% 0.1)
(def ^:private init-drake% 0.01)
(def ^:private init-necro% 0.1)
(def ^:private init-giant_amoeba% 0.1)

;; Starting floor for certain monsters to spawn on
(def ^:private init-skeleton-floor 2)
(def ^:private init-snake-floor 1)
(def ^:private init-troll-floor 4)
(def ^:private init-mimic-floor 5)
(def ^:private init-spider-floor 1)
(def ^:private init-slime-floor 3)
(def ^:private init-drake-floor 7)
(def ^:private init-necro-floor 6)
(def ^:private init-giant_amoeba-floor 5)
