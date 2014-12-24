(ns rouje-like.config
  (:require [clojure.set :refer [union]]
            [play-clj.core :refer [graphics!]]))

#_(use 'rouje-like.config :reload)

;; ===== WORLD CONFIG =====
(def padding-sizes {:top   3
                    :btm   2
                    :left  1
                    :right 1})

(def view-port-sizes [20 20])

(defn block-size []
  (let [min-block-size 36
        block-size (try (let [height (graphics! :get-height)
                              width  (graphics! :get-width)
                              [vp-x vp-y] view-port-sizes
                              {:keys [top btm left right]} padding-sizes]
                          (/ height (+ top btm vp-y)))
                        (catch Exception e
                          min-block-size))]
    (max min-block-size block-size)))

(def world-sizes {:width  20
                  :height 20})

;; ===== TILE TYPES =====
(def <floors>
  #{:open-door :dune
    :floor :forest-floor})

(def <walls>
  #{:temple-wall
    :door :wall
    :tree})

(def <indestructible-walls>
  #{:temple-wall :maze-wall})

(def <items>
  #{:torch :gold
    :health-potion
    :equipment
    :purchasable
    :merchant
    :magic-potion})

(def <empty>
  (union <floors> <items>))

(def <sight-blockers>
  (union <walls>
         <indestructible-walls>
         #{:arrow-trap :lichen}))

(def <valid-move-targets>
  (union <empty> #{:portal
                   :m-portal
                   :spike-trap
                   :hidden-spike-trap}))

(def <valid-mob-targets>
  (union <empty> #{:player}))

;; ===== PLAYER CONFIG =====
(def player-stats
  {:max-hp 100
   :max-mp 15
   :atk 4
   :def 1})

(def class->stats
  {:rogue   {:max-hp  2 :max-mp  2 :atk  1}
   :warrior {:max-hp  5 :max-mp -5 :atk  2}
   :mage    {:max-hp -5 :max-mp  5 :atk -2}})

(def race->stats
  {:human {:max-hp 10  :atk 1 :max-mp 0}
   :orc   {:max-hp 20  :atk 2 :max-mp -1}
   :elf   {:max-hp -5  :atk 0 :max-mp 3}})

(def stat->comp
  {:max-hp :destructible
   :hp :destructible
   :atk :attacker
   :def :destructible
   :max-mp :magic
   :mp :magic})

;; TODO add magic-atk
(def stat->pointinc
  {:max-hp 5
   :atk 1
   :def 1
   :max-mp 2})

(def spell->mp-cost
  {:fireball 3
   :powerattack 2
   :pickpocket 2})

(def class->spell
  {:mage [:fireball]
   :rogue [:pickpocket]
   :warrior [:powerattack]})

(def level-exp
  {:exp 3})

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
               :duration 4
               :value    1}
   :fireball  {:type     :fire
               :duration 2
               :value    2}})

(def spell-effects
  "In the spell map, :value's effect is based on what
  type of a spell it is, eg extra dmg or gold stolen."
  {:fireball     {:type :fire
                  :distance 3
                  :value 3}
   :powerattack  {:type :powerattack
                  :distance 1
                  :value 2}
   :pickpocket   {:type :pickpocket
                  :distance 1
                  :atk-reduction -2
                  :value 1}})

;; ===== CREATURE CONFIG =====
(def entity->stats
  {:wall            {:hp 2}
   :tree            {:hp 1}
   :maze-wall       {:hp 100}
   :temple-wall     {:hp 100}
   :arrow-trap      {:hp 1
                     :atk 2}
   :spike-trap      {:hp 1
                     :atk 2}
   :bat             {:hp 1
                     :def 0}
   :lichen          {:hp 4
                     :atk 0
                     :def 1}
   :skeleton        {:hp 8
                     :def 1
                     :atk 3
                     :exp 1}
   :snake           {:hp  2
                     :def 1
                     :atk 2
                     :exp 1}
   :troll           {:hp  8
                     :def 2
                     :atk 4
                     :exp 4}
   :mimic           {:hp 5
                     :def 2
                     :atk 3
                     :exp 3}
   :spider          {:hp 2
                     :def 0
                     :atk 1
                     :exp 1}
   :slime           {:hp 2
                     :def 0
                     :atk 1
                     :exp 1}
   :drake           {:hp 6
                     :def 3
                     :atk 4
                     :exp 5}
   :necro           {:hp 6
                     :def 3
                     :atk 4
                     :exp 5}
   :colossal-amoeba {:hp 6
                     :def 1
                     :atk 3
                     :exp 0
                     :split-rate 3}
   :giant-amoeba    {:hp 4
                     :def 1
                     :atk 2
                     :exp 0
                     :split-rate 2}
   :large-amoeba    {:hp 2
                     :def 1
                     :atk 1
                     :exp 2}
   :willow-wisp     {:hp 3
                     :def 0
                     :atk 0
                     :exp 1}
   :hydra-head      {:hp 25
                     :def 3
                     :atk 5
                     :exp 10}
   :hydra-neck      {:hp 20
                     :def 3
                     :atk 0
                     :exp 0}
   :hydra-tail      {:hp 20
                     :def 1
                     :atk 0
                     :exp 0}
   :hydra-rear      {:hp 20
                     :def 1
                     :atk 0
                     :exp 0}})

(def potion-stats
  {:health 5
   :magic 3})

;; ===== WORLD CONFIG =====
(def world-entity->spawn%
  {:wall  45
   :torch 2
   :gold  5
   :hp    1
   :mp    1
   :eq    1})

(def maze:wall->floor% 20)

;; ===== MONSTER CONFIG =====
(def mob->init-spawn%
  {:willow-wisp     0.4;0.1
   :giant-amoeba    0.4;0.1
   :colossal-amoeba 1
   :bat             1
   :lichen          1
   :necro           0.4;0.1
   :drake           0.4;0.01
   :slime           0.4;0.1
   :spider          0.4;0.5
   :boss            0.4;0.3
   :mimic           0.4;0.1
   :troll           0.4;0.1
   :snake           0.4;0.3
   :skeleton        0.4;0.1
   })

;; ===== STARTING FLOOR FOR MOBS =====
(def mob->init-floor
  {:skeleton      1;2
   :willow-wisp   1;3
   :giant-amoeba  1;5
   :necro         1;6
   :drake         1;8
   :slime         1;3
   :spider        1
   :mimic         1;4
   :troll         1;4
   :snake         1})

(def k->boss-config
  {:every-?-levels 2})

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
