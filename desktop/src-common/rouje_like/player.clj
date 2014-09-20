(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui   :refer :all]

            [rouje-like.entity :as rj.e]))

(defn move-player!
  [system direction]
  (let [e-player (first (rj.e/all-e-with-c system :player))

        c-score (rj.e/get-c-on-e system e-player :score)
        score (:score c-score) ;; atom!

        c-moves-left (rj.e/get-c-on-e system e-player :moves-left)
        moves-left (:moves-left c-moves-left) ;; atom!

        c-sight (rj.e/get-c-on-e system e-player :sight)
        sight-distance (:distance c-sight) ;; atom!

        c-player (rj.e/get-c-on-e system e-player :player)
        board (:tiles c-player) ;; atom!

        c-player-pos (rj.e/get-c-on-e system e-player :position)
        x-pos (:x c-player-pos) ;; atom!
        y-pos (:y c-player-pos) ;; atom!

        target-coords (case direction
                        :up    [     @x-pos (inc @y-pos)]
                        :down  [     @x-pos (dec @y-pos)]
                        :left  [(dec @x-pos)     @y-pos]
                        :right [(inc @x-pos)     @y-pos])
        target (get-in @board target-coords :bound)
        [target-x-pos target-y-pos] target-coords]
    (if (and (not= target :bound)
             (pos? @moves-left)
             (not= (:type target) :wall))
      (do
        (swap! moves-left dec)
        (swap! sight-distance (fn [prev] (if (> prev 3)     ;lower bound on sight
                                           (- prev (/ 1 3)) ;1 over the number of steps to lose 1 sight level
                                           prev)))
        (swap! board (fn [prev]
                       (-> prev
                           (update-in [@x-pos @y-pos]
                                      (fn [x] (assoc x :type :empty)))
                           (update-in target-coords
                                      (fn [x] (assoc x :type :player))))))
        (reset! x-pos target-x-pos)
        (reset! y-pos target-y-pos)
        (case (:type target)
          :gold     (do
                     (swap! score inc))
          :torch    (do
                      (swap! sight-distance
                             (fn [prev] (if (< prev 9)      ;upper bound on sight (ie 10)
                                          (+ prev 2)        ;torch benefits
                                          prev))))
          nil)))))

(defn render-player-stats
  [system _ {:keys [world-sizes, block-size]}]
  (let [[_ height] world-sizes

        e-score (first (rj.e/all-e-with-c system :score))
        c-score (rj.e/get-c-on-e system e-score :score)
        score @(:score c-score)

        e-moves-left (first (rj.e/all-e-with-c system :moves-left))
        c-moves-left (rj.e/get-c-on-e system e-moves-left :moves-left)
        moves-left @(:moves-left c-moves-left)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Score: [" score "]" " - " "MovesLeft: [" moves-left "]")
                   (color :white)
                   :set-y (float (* (inc height) block-size)))
            :draw renderer 1.0)
    (.end renderer)))

