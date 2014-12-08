(ns rouje-like.config
  (:require [clojure.set :refer [union]]
            [play-clj.core :refer [graphics!]]))

#_(use 'rouje-like.config :reload)

(def block-size 27)                                         ;; To see start screen, revert to 36
;; ===== WORLD CONFIG =====
(def padding-sizes {:top   3
                    :btm   2
                    :left  1
                    :right 1})

(def view-port-sizes [20 20])

(defn block-size []
  (let [min-block-size 27
        block-size (try (let [height (graphics! :get-height)
                              width  (graphics! :get-width)
                              [vp-x vp-y] view-port-sizes
                              {:keys [top btm left right]} padding-sizes]
                          (/ height (+ top btm vp-y)))
                        (catch Exception e
                          min-block-size))]
    (max min-block-size block-size)))

(def world-sizes {:width  60
                  :height 60})

;; ===== TILE TYPES =====
(def <floors>
  #{:open-door :dune :floor :forest-floor})

(def <walls>
  #{:temple-wall :door :wall :tree :maze-wall})

(def <indestructible-walls>
  #{:temple-wall :maze-wall})

(def <items>
  #{:torch :gold :health-potion :equipment :purchasable :merchant})

(def <empty>
  (union <floors> <items>))

(def <sight-blockers>
  (union <walls> #{:arrow-trap :lichen}))

(def <valid-move-targets>
  (union <empty> #{:portal :m-portal :spike-trap :hidden-spike-trap}))

(def <valid-mob-targets>
  (union <empty> #{:player}))

(def wall->stats
  {:wall        {:hp 2}
   :tree        {:hp 1}
   :maze-wall   {:hp 100}
   :temple-wall {:hp 100}})

(def trap->stats
  {:arrow-trap {:hp 1
                :atk 2}
   :spike-trap {:hp 1
                :atk 2}})

;; ===== PLAYER CONFIG =====
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

(def player-init-pos
  (let [x (/ (:width  world-sizes) 2)
        y (/ (:height world-sizes) 2)]
    [1 x y]))

(def player-sight
  {:distance 5.0
   :decline-rate (/ 1 4)
   :lower-bound 4         ;; Inclusive
   :upper-bound 11        ;; Exclusive
   :torch-multiplier 1.})

;; ===== EQUIPMENT CONFIG =====
(def weapons
  (->> (flatten
         [(repeat 1 [:sword  {:atk 3}])
          (repeat 1 [:mace   {:atk 2}])
          (repeat 1 [:axe    {:atk 3}])
          (repeat 1 [:flail  {:atk 2}])
          (repeat 1 [:dagger {:atk 1}])])
       (partition 2)))

(def weapon-qualities
  (->> (flatten
         [(repeat 1 [:quick  {:atk  1}])
          (repeat 1 [:giant  {:atk  2}])
          (repeat 1 [:great  {:atk  2}])
          (repeat 1 [:tiny   {:atk  1}])
          (repeat 1 [:dull   {:atk -1}])
          (repeat 1 [:dented {:atk -2}])])
       (partition 2)))

(def weapon-effects
  (->> (flatten
         [(repeat 1 [:bloodletting {:atk 1}])
          (repeat 1 [:pain {:atk 1}])
          (repeat 1 [:poison])
          (repeat 1 [:paralysis])
          (repeat 1 [:power {:atk 2}])
          (repeat 1 [:death {:atk 2}])
          (repeat 1 [:fire])
          (repeat 1 [nil])])
       (partition 2)))

(def armors
  (->> (flatten
         [(repeat 1 [:chestplate {:max-hp 1 :def 1}])
          (repeat 1 [:chainmail  {:max-hp 3}])
          (repeat 1 [:tunic      {:max-hp 1}])])
       (partition 2)))

(def status-effects
  {:paralysis {:type     :paralysis
               :duration 2
               :value    1}
   :poison    {:type     :poison
               :duration 2
               :value    2}
   :fire      {:type     :fire
               :duration 2
               :value    2}})

;; ===== CREATURE CONFIG =====
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

(def willowisp-stats
  {:hp 3
   :def 0
   :atk 0
   :exp 1})

(def hydra-head-stats ;;Currently using giant_amoeba stats.
  {:hp 4                                                    ;;Reconfigure for all 3 parts
   :def 1
   :atk 2
   :exp 0})

(def hydra-neck-stats
  {:hp 4
   :def 1
   :atk 0
   :exp 0})

(def hydra-tail-stats
  {:hp 4
   :def 1
   :atk 0
   :exp 0})

(def trap-types
  [:arrow])

(def potion-stats
  {:health 5})

;; ===== WORLD CONFIG =====
(def init-wall% 45)
(def init-torch% 2)
(def init-gold% 5)
(def init-health-potion% 2)
(def init-lichen% 1)
(def init-bat% 1)
(def init-equip% 1)

;; ===== MONSTER CONFIG =====
(def init-skeleton% 0.1)
(def init-snake% 0.3)
(def init-troll% 0.1)
(def init-mimic% 0.1)
(def init-spider% 0.5)
(def init-slime% 0.1)
(def init-drake% 0.01)
(def init-necro% 0.1)
(def init-giant_amoeba% 0.1)                                ;;FOR EASY TESTING, SET TO 1
(def init-willowisp% 0.1)
(def init-hydra% 0.3)

;; ===== Starting floor for certain monsters to spawn on =====
(def init-skeleton-floor 2)
(def init-snake-floor 1)
(def init-troll-floor 4)
(def init-mimic-floor 4)
(def init-spider-floor 1)
(def init-slime-floor 3)
(def init-drake-floor 8)
(def init-necro-floor 6)
(def init-giant_amoeba-floor 5)                             ;;FOR EASY TESTING: SET TO 1
(def init-willowisp-floor 3)

;; ===== MERCHANT CONFIG =====
(def merchant-pos
  {:x 10
   :y 12})

(def merchant-portal-pos
  {:x 10
   :y 14})

(def merchant-player-pos
  {:x 10
   :y 8})

(def merchant-level-size
  {:width 10
   :height 10})

(def merchant-item-pos
  [{:x 8  :y 10}
   {:x 10 :y 10}
   {:x 12 :y 10}])

(def inspectables
  [:purchasable :merchant])
