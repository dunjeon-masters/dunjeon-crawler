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
         (#{:wall} (:type (first (:entities target)))))))

(defn dig!
  [system this target]
  (let [c-position (rj.e/get-c-on-e system this :position)
        world (:world c-position)#_(ATOM)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)#_(ATOM)

        wall->floor (fn [world ks]
                      (update-in world ks
                                 (fn [tile]
                                   (update-in tile [:entities]
                                              (fn [entities]
                                                (map (fn [entity]
                                                       (if (#{:wall} (:type entity))
                                                         (rj.c/map->Entity {:type :floor})
                                                         entity))
                                                     entities))))))]
    (do
      (swap! world wall->floor [(:x target) (:y target)])
      (swap! moves-left dec))))

(defn can-move?
  [system this target]
  (let [c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)]
    (and (pos? moves-left)
         (#{:floor :gold :torch} (:type (first (:entities target)))))))

(defn move!
  [system this target]
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

        c-position (rj.e/get-c-on-e system this :position)
        world (:world c-position)#_(ATOM)
        x-pos (:x c-position)#_(ATOM)
        y-pos (:y c-position)#_(ATOM)

        player<->tile (fn [world ks]
                         (-> world
                             (update-in ks
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (map (fn [entity]
                                                              (if (#{:player} (:type entity))
                                                                (rj.c/map->Entity {:type :floor})
                                                                entity))
                                                            entities)))))
                             (update-in [(:x target) (:y target)]
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (map (fn [entity]
                                                              (if (#{:floor :gold :torch} (:type entity))
                                                                (rj.c/map->Entity {:type :player})
                                                                entity))
                                                            entities)))))))]
    (swap! moves-left dec)
    (case (:type (first (:entities target)))
      ;; TODO: Try each case without a do block
      :gold  (do
               (swap! gold inc)
               (swap! sight-distance dec-sight))
      :torch (do
               (swap! sight-distance inc-sight))
      :floor (do
               (swap! sight-distance dec-sight))
      nil)
    (swap! world player<->tile [@x-pos @y-pos])
    (reset! x-pos (:x target))
    (reset! y-pos (:y target))))

(defn process-input-tick!
  [system direction]
  (let [this (first (rj.e/all-e-with-c system :player))

        c-position (rj.e/get-c-on-e system this :position)
        world (:world c-position) ;; atom!
        x-pos (:x c-position) ;; atom!
        y-pos (:y c-position) ;; atom!

        c-mobile (rj.e/get-c-on-e system this :mobile)

        target-coords (case direction
                        :up    [     @x-pos (inc @y-pos)]
                        :down  [     @x-pos (dec @y-pos)]
                        :left  [(dec @x-pos)     @y-pos]
                        :right [(inc @x-pos)     @y-pos])
        target (get-in @world target-coords {:entities [{:type :bound}]})

        digger (rj.e/get-c-on-e system this :digger)]
    (cond
      ((:can-move? c-mobile) system this target)
      ((:move! c-mobile) system this target)

      ((:can-dig? digger) system this target)
      ((:dig! digger) system this target))))

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
