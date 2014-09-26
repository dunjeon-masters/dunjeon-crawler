(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer [texture texture!]]

            [rouje-like.components :as rj.c :refer [can-attack? attack]]
            [rouje-like.entity :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.world :as rj.wr]))

(defn can-dig?
  [_ _ target]
  (#{:wall} (:type (rj.u/get-top-entity target))))

(defn dig
  [system e-this target-tile]
  (let [e-world (first (rj.e/all-e-with-c system :world))]
    (-> system
        (rj.wr/update-in-world e-world [(:x target-tile) (:y target-tile)]
                               (fn [entities _]
                                 (remove #(#{:wall} (:type %))
                                         entities)))
        (rj.e/upd-c e-this :moves-left
                    (fn [c-moves-left]
                      (update-in c-moves-left [:moves-left] dec))))))

(defn can-move?
  [_ _ target-tile]
  (#{:floor :gold :torch} (:type (rj.u/get-top-entity target-tile))))

(defn move
  [system e-this target-tile]
  (let [c-sight (rj.e/get-c-on-e system e-this :sight)
        sight-decline-rate (:decline-rate c-sight)
        sight-lower-bound (:lower-bound c-sight)
        sight-upper-bound (:upper-bound c-sight)
        sight-torch-power (:torch-power c-sight)
        dec-sight (fn [prev] (if (> prev (inc sight-lower-bound))
                               (- prev sight-decline-rate)
                               prev))
        inc-sight (fn [prev] (if (<= prev (- sight-upper-bound sight-torch-power))
                               (+ prev sight-torch-power)
                               prev))

        c-position (rj.e/get-c-on-e system e-this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (-> system
        (rj.e/upd-c e-this :moves-left
                    (fn [c-moves-left]
                      (update-in c-moves-left [:moves-left] dec)))

        (as-> system
              (case (:type (rj.u/get-top-entity target-tile))
                :gold  (-> system
                           (rj.e/upd-c e-this :gold
                                       (fn [c-gold]
                                         (update-in c-gold [:gold] inc)))
                           (rj.e/upd-c e-this :sight
                                       (fn [c-sight]
                                         (update-in c-sight [:distance] dec-sight))))
                :torch (-> system
                           (rj.e/upd-c e-this :sight
                                       (fn [c-sight]
                                         (update-in c-sight [:distance] inc-sight))))
                :floor (-> system
                           (rj.e/upd-c e-this :sight
                                       (fn [c-sight]
                                         (update-in c-sight [:distance] dec-sight))))
                system))

        (rj.wr/update-in-world e-world [(:x target-tile) (:y target-tile)]
                               (fn [entities _]
                                 (vec
                                   (conj
                                     (remove #(#{:gold :torch} (:type %))
                                             entities)
                                     (rj.c/map->Entity {:type :player
                                                        :id   e-this})))))
        (rj.wr/update-in-world e-world [(:x c-position) (:y c-position)]
                               (fn [entities _]
                                 (vec
                                   (remove
                                     #(#{:player} (:type %))
                                     entities))))

        (rj.e/upd-c e-this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target-tile))
                          (assoc-in [:y] (:y target-tile))))))))

(defn process-input-tick
  [system direction]
  (let [this (first (rj.e/all-e-with-c system :player))

        c-position (rj.e/get-c-on-e system this :position)
        x-pos (:x c-position)
        y-pos (:y c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)
        target-coords (rj.u/offset-coords [x-pos y-pos]
                                          (rj.u/direction->offset
                                            direction))
        target-tile (get-in world target-coords nil)]
    (if (and (not (nil? target-tile)))
      (let [c-mobile   (rj.e/get-c-on-e system this :mobile)
            c-digger   (rj.e/get-c-on-e system this :digger)
            c-attacker (rj.e/get-c-on-e system this :attacker)]
        (cond
          ((:can-move? c-mobile) system this target-tile)
          ((:move c-mobile) system this target-tile)

          ((:can-dig? c-digger) system this target-tile)
          ((:dig c-digger) system this target-tile)

          (can-attack? c-attacker this target-tile system)
          (attack c-attacker this target-tile system)))
      system)))

;;RENDERING FUNCTIONS
(defn render-player-stats
  [_ e-this {:keys [view-port-sizes]} system]
  (let [[_ vheight] view-port-sizes

        c-position (rj.e/get-c-on-e system e-this :position)
        x (:x c-position)
        y (:y c-position)

        c-gold (rj.e/get-c-on-e system e-this :gold)
        gold (:gold c-gold)

        c-moves-left (rj.e/get-c-on-e system e-this :moves-left)
        moves-left (:moves-left c-moves-left)

        c-destructible (rj.e/get-c-on-e system e-this :destructible)
        hp (:hp c-destructible)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Gold: [" gold "]" " - " "MovesLeft: [" moves-left "]"
                        " - " "Position: [" x "," y "]" " - " "HP: [" hp "]")
                   (color :green)
                   :set-y (float (* (+ vheight
                                       (dec (* 2 (:y rj.c/padding-sizes))))
                                    rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [_ e-this args system]
  (render-player-stats _ e-this args system))
