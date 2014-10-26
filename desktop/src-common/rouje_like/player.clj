(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer [texture texture!]]

            [rouje-like.components :as rj.c :refer [can-attack? attack
                                                    can-move? move]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.mobile :as rj.m]
            [brute.entity :as br.e]
            [rouje-like.config :as rj.cfg]))

(defn can-dig?
  [_ target]
  (#{:wall} (:type (rj.u/tile->top-entity target))))

(defn dig
  [system target-tile]
  (let [e-world (first (rj.e/all-e-with-c system :world))]
    (-> system
        (rj.u/update-in-world e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                               (fn [entities]
                                 (remove #(#{:wall} (:type %))
                                         entities))))))

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
      (-> (let [c-mobile   (rj.e/get-c-on-e system e-this :mobile)
                c-digger   (rj.e/get-c-on-e system e-this :digger)
                c-attacker (rj.e/get-c-on-e system e-this :attacker)
                e-target (:id (rj.u/tile->top-entity target-tile))]
            (cond
              (can-move? c-mobile e-this target-tile system)
              (move c-mobile e-this target-tile system)

              ((:can-dig?-fn c-digger) system target-tile)
              ((:dig-fn c-digger) system target-tile)

              (can-attack? c-attacker e-this e-target system)
              (attack c-attacker e-this e-target system)

              :else system))
          (rj.e/upd-c e-this :playersight
                      (fn [c-playersight]
                        (update-in c-playersight [:distance] dec-sight)))
          (as-> system
                (let [c-position (rj.e/get-c-on-e system e-this :position)
                      this-pos [(:z c-position) (:x c-position) (:y c-position)]
                      this-tile (get-in levels this-pos)

                      ;;TODO: There might be multiple items & user might want to choose to not pickup
                      item (first (filter #(rj.e/get-c-on-e system (:id %) :item)
                                          (:entities this-tile)))]
                  (if item
                    (let [e-item (:id item)
                          c-item (rj.e/get-c-on-e system e-item :item)]
                      ((:pickup-fn c-item) system e-this e-item this-pos (:type item)))
                    system))))
      system)))

(def ^:private init-player-x-pos (/ (:width  rj.c/world-sizes) 2))
(def ^:private init-player-y-pos (/ (:height rj.c/world-sizes) 2))
(def init-player-pos [0 init-player-x-pos init-player-y-pos])
(def ^:private init-sight-distance 5.0)
(def ^:private init-sight-decline-rate (/ 1 4))
(def ^:private init-sight-lower-bound 4)                    ;; Inclusive
(def ^:private init-sight-upper-bound 11)                   ;; Exclusive
(def ^:private init-sight-torch-multiplier 1.)

(declare render-player)
(defn init-player
  [system]
  (let [e-player (br.e/create-entity)
        player-class (rand-nth (keys rj.cfg/class->stats))
        player-race (rand-nth (keys rj.cfg/race->stats))]
    (-> system
        (rj.e/add-e e-player)
        (rj.e/add-c e-player (rj.c/map->Player {:show-world? false}))
        (rj.e/add-c e-player (rj.c/map->Class- {:class player-class}))
        (rj.e/add-c e-player (rj.c/map->Race {:race player-race}))
        (rj.e/add-c e-player (rj.c/map->Experience {:experience 0}))
        (rj.e/add-c e-player (rj.c/map->Position {:x init-player-x-pos
                                                  :y init-player-y-pos
                                                  :z 0
                                                  :type :player}))
        (rj.e/add-c e-player (rj.c/map->Mobile {:can-move?-fn rj.m/can-move?
                                                :move-fn      rj.m/move}))
        (rj.e/add-c e-player (rj.c/map->Digger {:can-dig?-fn can-dig?
                                                :dig-fn      dig}))
        (rj.e/add-c e-player (rj.c/map->Attacker {:atk              (+ (:atk rj.cfg/player-stats) (:atk (rj.cfg/race->stats player-race)))
                                                  :can-attack?-fn   rj.atk/can-attack?
                                                  :attack-fn        rj.atk/attack
                                                  :is-valid-target? (constantly true)}))
        (rj.e/add-c e-player (rj.c/map->Wallet {:gold 0}))
        (rj.e/add-c e-player (rj.c/map->PlayerSight {:distance (inc init-sight-distance)
                                                     :decline-rate  init-sight-decline-rate
                                                     :lower-bound   init-sight-lower-bound
                                                     :upper-bound   init-sight-upper-bound
                                                     :torch-multiplier   init-sight-torch-multiplier}))
        (rj.e/add-c e-player (rj.c/map->Renderable {:render-fn render-player
                                                    :args      {:view-port-sizes rj.c/view-port-sizes}}))
        (rj.e/add-c e-player (rj.c/map->Destructible {:hp      (+ (:hp rj.cfg/player-stats) (:hp (rj.cfg/race->stats player-race)))
                                                      :defense (:def rj.cfg/player-stats)
                                                      :can-retaliate? false
                                                      :take-damage-fn rj.d/take-damage}))
        (rj.e/add-c e-player (rj.c/map->Broadcaster {:msg-fn (constantly "you")})))))

(defn render-player-stats
  [_ e-this {:keys [view-port-sizes]} system]
  (let [[_ vheight] view-port-sizes

        c-race (rj.e/get-c-on-e system e-this :race)
        race (:race c-race)

        c-wallet (rj.e/get-c-on-e system e-this :wallet)
        gold (:gold c-wallet)

        c-experience (rj.e/get-c-on-e system e-this :experience)
        experience (:experience c-experience)

        c-position (rj.e/get-c-on-e system e-this :position)
        x (:x c-position)
        y (:y c-position)
        z (:z c-position)
        c-destructible (rj.e/get-c-on-e system e-this :destructible)
        hp (:hp c-destructible)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Gold: [" gold "]"
                        " - " "Position: [" x "," y "," z "]"
                        " - " "HP: [" hp "]"
                        " - " "Race: [" race "]"
                        " - " "Experience: [" experience "]")

                   (color :green)
                   :set-y (float (* (+ vheight
                                       (dec (+ (:top rj.c/padding-sizes)
                                               (:btm rj.c/padding-sizes))))
                                    rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [_ e-this args system]
  (render-player-stats _ e-this args system))
