(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]))

(defn move-player!
  [system direction]
  (let [e-player (first (rj.e/all-e-with-c system :player))

        c-gold (rj.e/get-c-on-e system e-player :gold)
        gold (:gold c-gold) ;; atom!

        c-moves-left (rj.e/get-c-on-e system e-player :moves-left)
        moves-left (:moves-left c-moves-left) ;; atom!

        c-sight (rj.e/get-c-on-e system e-player :sight)
        sight-distance (:distance c-sight) ;; atom!
        sight-decline-rate @(:decline-rate c-sight)
        sight-lower-bound @(:lower-bound c-sight)
        sight-upper-bound @(:upper-bound c-sight)

        c-player (rj.e/get-c-on-e system e-player :player)
        world (:tiles c-player) ;; atom!

        c-player-pos (rj.e/get-c-on-e system e-player :position)
        x-pos (:x c-player-pos) ;; atom!
        y-pos (:y c-player-pos) ;; atom!

        target-coords (case direction
                        :up    [     @x-pos (inc @y-pos)]
                        :down  [     @x-pos (dec @y-pos)]
                        :left  [(dec @x-pos)     @y-pos]
                        :right [(inc @x-pos)     @y-pos])
        target (get-in @world target-coords {:type :wall})
        [target-x-pos target-y-pos] target-coords]
    (if (and (pos? @moves-left)
             (not= (:type target)
                   :wall))
      (do
        (swap! moves-left dec)
        (swap! sight-distance (fn [prev] (if (> prev (inc sight-lower-bound))
                                           (- prev sight-decline-rate)
                                           prev)))
        (swap! world (fn [prev]
                       (-> prev
                           (update-in [@x-pos @y-pos]
                                      (fn [x] (assoc x :type :floor)))
                           (update-in target-coords
                                      (fn [x] (assoc x :type :player))))))
        (reset! x-pos target-x-pos)
        (reset! y-pos target-y-pos)
        (case (:type target)
          :gold     (do
                      (swap! gold inc))
          :torch    (do
                      (swap! sight-distance
                             (fn [prev] (if (<= prev (- sight-upper-bound 2))
                                          (+ prev 2)
                                          prev))))
          nil)))))

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
                   :set-y (float (* (inc height) rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [system this args]
  (render-player-stats system this args))
