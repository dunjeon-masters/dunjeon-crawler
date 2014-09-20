(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]))

(defn can-dig?
  [system this target]
  (let [c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)]
    (and (pos? moves-left)
         (#{:wall} (:type target)))))

(defn dig!
  [system this _ target-coords]
  (let [c-player (rj.e/get-c-on-e system this :player)
        world (:tiles c-player)#_(ATOM)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)#_(ATOM)

        wall->floor (fn [prev ks]
                      (update-in prev ks
                                 (fn [x] (assoc x :type :floor))))]
    (do
      (swap! world wall->floor target-coords)
      (swap! moves-left dec))))

(defn can-move?
  [system this target]
  (let [c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)]
    (and (pos? moves-left)
         (#{:floor :gold :torch} (:type target)))))

(defn move!
  [system this target target-coords]
  (let [c-gold (rj.e/get-c-on-e system this :gold)
        gold (:gold c-gold)#_(ATOM)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)#_(ATOM)

        c-sight (rj.e/get-c-on-e system this :sight)
        sight-distance (:distance c-sight)#_(ATOM)
        sight-decline-rate @(:decline-rate c-sight)
        sight-lower-bound @(:lower-bound c-sight)
        sight-upper-bound @(:upper-bound c-sight)
        dec-sight (fn [prev] (if (> prev (inc sight-lower-bound))
                               (- prev sight-decline-rate)
                               prev))
        inc-sight (fn [prev] (if (<= prev (- sight-upper-bound 2))
                               (+ prev 2)
                               prev))

        c-player (rj.e/get-c-on-e system this :player)
        world (:tiles c-player)#_(ATOM)

        c-player-pos (rj.e/get-c-on-e system this :position)
        x-pos (:x c-player-pos)#_(ATOM)
        y-pos (:y c-player-pos)#_(ATOM)

        [target-x-pos target-y-pos] target-coords

        player<->floor (fn [prev]
                         (-> prev
                             (update-in [@x-pos @y-pos]
                                        (fn [x] (assoc x :type :floor)))
                             (update-in target-coords
                                        (fn [x] (assoc x :type :player)))))]
    (case (:type target)
      :gold (do
              (swap! moves-left dec)
              (swap! gold inc)
              (swap! sight-distance dec-sight)
              (swap! world player<->floor)
              (reset! x-pos target-x-pos)
              (reset! y-pos target-y-pos))
      :torch (do
               (swap! moves-left dec)
               (swap! sight-distance inc-sight)
               (swap! world player<->floor)
               (reset! x-pos target-x-pos)
               (reset! y-pos target-y-pos))
      :floor (do
               (swap! moves-left dec)
               (swap! sight-distance dec-sight)
               (swap! world player<->floor)
               (reset! x-pos target-x-pos)
               (reset! y-pos target-y-pos))
      nil)))

(defn process-input-tick!
  [system direction]
  (let [e-player (first (rj.e/all-e-with-c system :player))

        c-player (rj.e/get-c-on-e system e-player :player)
        world (:tiles c-player) ;; atom!

        player-pos (rj.e/get-c-on-e system e-player :position)
        x-pos (:x player-pos) ;; atom!
        y-pos (:y player-pos) ;; atom!

        target-coords (case direction
                        :up    [     @x-pos (inc @y-pos)]
                        :down  [     @x-pos (dec @y-pos)]
                        :left  [(dec @x-pos)     @y-pos]
                        :right [(inc @x-pos)     @y-pos])
        target (get-in @world target-coords {:type :bound})

        digger (rj.e/get-c-on-e system e-player :digger)]
    (cond
      ((:can-move? player-pos) system e-player target)
      ((:move! player-pos) system e-player target target-coords)

      ((:can-dig? digger) system e-player target)
      ((:dig! digger) system e-player target target-coords))))

;;RENDERING FUNCTIONS
(defn render-player-stats
  [system this {:keys [world-sizes]}]
  (let [[_ height] world-sizes

        c-gold (rj.e/get-c-on-e system this :gold)
        gold @(:gold c-gold)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Gold: [" gold "]" " - " "MovesLeft: [" moves-left "]")
                   (color :white)
                   :set-y (float (* (+ height 2) rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [system this args]
  (render-player-stats system this args))
