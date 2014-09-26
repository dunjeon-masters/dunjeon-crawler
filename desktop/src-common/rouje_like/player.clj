(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer [texture texture!]]

            [rouje-like.components :as rj.c :refer [can-attack? attack
                                                    can-move? move]]
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

(defn process-input-tick
  [system direction]
  (let [e-this (first (rj.e/all-e-with-c system :player))

        c-position (rj.e/get-c-on-e system e-this :position)
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
      (let [c-mobile   (rj.e/get-c-on-e system e-this :mobile)
            c-digger   (rj.e/get-c-on-e system e-this :digger)
            c-attacker (rj.e/get-c-on-e system e-this :attacker)]
        (cond
          (can-move? c-mobile e-this target-tile system)
          (move c-mobile e-this target-tile system)

          ((:can-dig?-fn c-digger) system e-this target-tile)
          ((:dig-fn c-digger) system e-this target-tile)

          (can-attack? c-attacker e-this target-tile system)
          (attack c-attacker e-this target-tile system)))
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
    (label! (label (str "Gold: [" gold "]"
                        " - " "MovesLeft: [" moves-left "]"
                        " - " "Position: [" x "," y "]"
                        " - " "HP: [" hp "]")
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
