(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [rouje-like.components :as rj.c :refer [can-attack? attack
                                                    can-move? move]]
            [rouje-like.rendering :as rj.r]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.destructible :as rj.d]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.mobile :as rj.m]
            [brute.entity :as br.e]
            [rouje-like.experience :as rj.exp]
            [rouje-like.config :as rj.cfg]))

(defn can-dig?
  [_ _ target]
  (rj.cfg/<walls> (:type (rj.u/tile->top-entity target))))

(defn dig
  [system e-this target-tile]
  (let [target-top-entity (rj.u/tile->top-entity target-tile)
        damage 1
        e-target (:id target-top-entity)
        c-destr (rj.e/get-c-on-e system e-target :destructible)]
    (rj.c/take-damage c-destr e-target damage e-this system)))

(defn process-input-tick
  [system direction]
  (let [e-this (first (rj.e/all-e-with-c system :player))

        c-playersight (rj.e/get-c-on-e system e-this :playersight)
        sight-decline-rate (:decline-rate c-playersight)
        sight-lower-bound (:lower-bound c-playersight)
        dec-sight (fn [prev] (if (> prev (inc sight-lower-bound))
                               (- prev sight-decline-rate)
                               prev))

        c-position (rj.e/get-c-on-e system e-this :position)
        x-pos (:x c-position)
        y-pos (:y c-position)
        z-pos (:z c-position)
        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        world (nth levels z-pos)

        target-coords (rj.u/coords+offset [x-pos y-pos]
                                          (rj.u/direction->offset
                                            direction))
        target-tile (get-in world target-coords nil)]
    (if (and (not (nil? target-tile)))
      (as-> (let [c-mobile   (rj.e/get-c-on-e system e-this :mobile)
                  c-digger   (rj.e/get-c-on-e system e-this :digger)
                  c-attacker (rj.e/get-c-on-e system e-this :attacker)
                  e-target (:id (rj.u/tile->top-entity target-tile))]
              (cond
                (can-move? c-mobile e-this target-tile system)
                (move c-mobile e-this target-tile system)

                ((:can-dig?-fn c-digger) system e-this target-tile)
                ((:dig-fn c-digger) system e-this target-tile)

                (can-attack? c-attacker e-this e-target system)
                (attack c-attacker e-this e-target system)

                :else system)) system
        (rj.d/apply-effects system e-this)
        (rj.e/upd-c system e-this :playersight
                    (fn [c-playersight]
                      (update-in c-playersight [:distance] dec-sight)))
        (let [c-position (rj.e/get-c-on-e system e-this :position)
              this-pos [(:z c-position) (:x c-position) (:y c-position)]
              this-tile (get-in levels this-pos)

              ;;TODO: There might be multiple items & user might want to choose
              ;;to not pickup
              item (first (filter #(rj.e/get-c-on-e system (:id %) :item)
                                  (:entities this-tile)))]
          (if item
            (let [e-item (:id item)
                  c-item (rj.e/get-c-on-e system e-item :item)]
              ((:pickup-fn c-item) system e-this e-item this-pos (:type item)))
            system))
        (rj.e/upd-c system e-this :energy
                    (fn [c-energy]
                      (update-in c-energy [:energy] dec))))
      system)))

(def ^:private init-player-x-pos (/ (:width  rj.cfg/world-sizes) 2))
(def ^:private init-player-y-pos (/ (:height rj.cfg/world-sizes) 2))
(def init-player-pos [0 init-player-x-pos init-player-y-pos])
(def ^:private init-sight-distance 5.0)
(def ^:private init-sight-decline-rate (/ 1 4))
(def ^:private init-sight-lower-bound 4)                    ;; Inclusive
(def ^:private init-sight-upper-bound 11)                   ;; Exclusive
(def ^:private init-sight-torch-multiplier 1.)

(defn init-player
  [system {:keys [n r c] :or {n "the player"} :as user}]
  (let [e-player (br.e/create-entity)

        valid-class? (into #{} (keys rj.cfg/class->stats))
        player-class (if (valid-class? (keyword c))
                       (keyword c) (rand-nth (keys rj.cfg/class->stats)))

        valid-race? (into #{} (keys rj.cfg/race->stats))
        player-race (if (valid-race? (keyword r))
                      (keyword r) (rand-nth (keys rj.cfg/race->stats)))

        max-hp (+ (:max-hp rj.cfg/player-stats)
                  (:max-hp (rj.cfg/race->stats player-race)))]
    (rj.e/system<<components
      system e-player
      [[:player {:name n
                 :show-world? false}]
       [:klass {:class player-class}]
       [:race {:race player-race}]
       [:experience {:experience 0
                     :level 1
                     :level-up-fn rj.exp/level-up}]
       [:position {:x init-player-x-pos
                   :y init-player-y-pos
                   :z 0
                   :type :player}]
       [:equipment {:weapon nil
                    :armor nil}]
       [:inventory {:slot nil :junk []}]
       [:energy {:energy 1}]
       [:mobile {:can-move?-fn rj.m/can-move?
                 :move-fn      rj.m/move}]
       [:digger {:can-dig?-fn can-dig?
                 :dig-fn      dig}]
       [:attacker {:atk              (+ (:atk rj.cfg/player-stats)
                                        (:atk (rj.cfg/race->stats player-race)))
                   :status-effects   [{:type :burn :duration 3
                                       :value 1
                                       :apply-fn rj.stef/apply-poison
                                       :e-from e-player}]
                   :can-attack?-fn   rj.atk/can-attack?
                   :attack-fn        rj.atk/attack
                   :is-valid-target? (constantly true)}]
       [:wallet {:gold 0}]
       [:player-sight {:distance (inc init-sight-distance)
                       :decline-rate  init-sight-decline-rate
                       :lower-bound   init-sight-lower-bound
                       :upper-bound   init-sight-upper-bound
                       :torch-multiplier   init-sight-torch-multiplier}]
       [:renderable {:render-fn rj.r/render-player
                     :args      {:view-port-sizes rj.cfg/view-port-sizes}}]
       [:destructible {:max-hp max-hp
                       :hp  max-hp
                       :def (:def rj.cfg/player-stats)
                       :can-retaliate? false
                       :take-damage-fn rj.d/take-damage
                       :status-effects []}]
       [:broadcaster {:name-fn (constantly "you")}]])))
