(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [rouje-like.components :as rj.c :refer [can-attack? attack
                                                    can-move? move
                                                    ->3DPoint]]
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

        {:keys [x y z]} (rj.e/get-c-on-e system e-this :position)

        e-world (first (rj.e/all-e-with-c system :world))
        {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
        world (nth levels z)

        target-coords (rj.u/coords+offset [x y]
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
        (let [this-pos (->3DPoint (rj.e/get-c-on-e system e-this :position))
              this-tile (get-in levels this-pos)

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

(defn init-player
  [system {:keys [n r c] :or {n "the player"} :as user}]
  (let [e-player (br.e/create-entity)

        [z x y] rj.cfg/player-init-pos
        {:keys [distance decline-rate
                lower-bound upper-bound
                torch-multiplier]} rj.cfg/player-sight
        _ (? torch-multiplier)

        valid-class? (into #{} (keys rj.cfg/class->stats))
        player-class (if (valid-class? (keyword c))
                       (keyword c) (rand-nth (keys rj.cfg/class->stats)))

        valid-race? (into #{} (keys rj.cfg/race->stats))
        player-race (if (valid-race? (keyword r))
                      (keyword r) (rand-nth (keys rj.cfg/race->stats)))

        max-hp (+ (:max-hp rj.cfg/player-stats)
                  (:max-hp (rj.cfg/race->stats player-race))
                  (:max-hp (rj.cfg/class->stats player-class)))

        max-mp (+ (:max-mp rj.cfg/player-stats)
                  (:max-mp (rj.cfg/race->stats player-race))
                  (:max-mp (rj.cfg/class->stats player-class)))

        cfg-mage-spells (rj.cfg/class->spell :mage)
        spell (rand-nth cfg-mage-spells)
        cfg-spell-effect (rj.cfg/spell-effects spell)
        spell (if (= player-class :mage)
                (conj [] {:name spell
                          :distance (:distance cfg-spell-effect)
                          :value (:value cfg-spell-effect)
                          :type (:type cfg-spell-effect)
                          :atk-reduction (:atk-reduction cfg-spell-effect)})
                [])]
    (rj.e/system<<components
      system e-player
      [[:player {:name n
                 :fog-of-war? true}]
       [:klass {:class player-class}]
       [:race {:race player-race}]
       [:experience {:experience 1
                     :level 1
                     :level-up-fn rj.exp/level-up}]
       [:position {:x x
                   :y y
                   :z z
                   :type :player}]
       [:equipment {:weapon nil
                    :armor nil}]
       [:inventory {:slot nil :junk []
                    :hp-potion 0 :mp-potion 0}]
       [:energy {:energy 1}]
       [:mobile {:can-move?-fn rj.m/can-move?
                 :move-fn      rj.m/move}]
       [:digger {:can-dig?-fn can-dig?
                 :dig-fn      dig}]
       [:attacker {:atk              (+ (:atk rj.cfg/player-stats)
                                        (:atk (rj.cfg/race->stats player-race))
                                        (:atk (rj.cfg/class->stats player-class)))
                   :status-effects   []
                   :can-attack?-fn   rj.atk/can-attack?
                   :attack-fn        rj.atk/attack
                   :is-valid-target? (constantly true)}]
       [:wallet {:gold 0}]
       [:player-sight {:distance    (inc distance)
                       :decline-rate     decline-rate
                       :lower-bound      lower-bound
                       :upper-bound      upper-bound
                       :torch-multiplier torch-multiplier}]
       [:renderable {:render-fn rj.r/render-player
                     :args      {:view-port-sizes rj.cfg/view-port-sizes}}]
       [:destructible {:max-hp max-hp
                       :hp  max-hp
                       :def (:def rj.cfg/player-stats)
                       :can-retaliate? false
                       :take-damage-fn rj.d/take-damage
                       :status-effects []
                       :on-death-fn (fn [_ _ s] s)}]
       [:magic {:max-mp max-mp
                :mp max-mp
                :spells spell}]
       [:broadcaster {:name-fn (constantly n)}]])))

(defn add-player
  [system]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        e-world (first (rj.e/all-e-with-c system :world))]
    (rj.u/update-in-world system e-world rj.cfg/player-init-pos
                          (fn [entities]
                            (vec (conj (filter #(rj.cfg/<floors> (:type %)) entities)
                                       (rj.c/map->Entity {:id   e-player
                                                          :type :player})))))))
