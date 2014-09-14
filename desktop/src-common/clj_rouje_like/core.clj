(ns clj-rouje-like.core
  (:import (com.badlogic.gdx.scenes.scene2d.ui Label))
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]))

(declare main-screen clj-rouje-like)

(def score (atom 0))
(def moves-left (atom 20))
(def block-size 27)
(def world-sizes [20 20])

(defn get-in-world
  [world x y]
  (first
    (filter (fn [e] (and
                      (= x (:x e))
                      (= y (:y e))))
            world)))

(defn replace-in-world
  "Replaces the object at x and y with obj"
  [world x y obj & args]
  (let [{:keys [type]} args]
    (merge (filter (fn [e] (or
                             (not= x (:x e))
                             (not= y (:y e))))
                   world)
           (assoc obj :x x :y y :type type))))

(defn swap-in-world
  [world x1 y1 x2 y2]
  (let [obj1 (get-in-world world x1 y1)
        obj2 (get-in-world world x2 y2)]
    (case (:type obj2)
      :wall world

      :gold (do
              (swap! score inc)
              (swap! moves-left dec)
              (-> world
                  (replace-in-world x1 y1
                                    (texture "period.jpg")
                                    :type :empty)
                  (replace-in-world x2 y2
                                    obj1 :type (:type obj1))))

      (let [new-world (-> world
                          (replace-in-world x1 y1 obj2 :type (:type obj2))
                          (replace-in-world x2 y2 obj1 :type (:type obj1)))]
        (when (identity new-world)
                (swap! moves-left dec)
                new-world)))))

(defn move-in
  [world entity direction]
  (let [x (:x entity)
        y (:y entity)
        new-world (case direction
                    :up    (swap-in-world world x y x (+ y block-size))
                    :down  (swap-in-world world x y x (- y block-size))
                    :right (swap-in-world world x y (+ x block-size) y)
                    :left  (swap-in-world world x y (- x block-size) y)
                    nil)]
    new-world))

(defn init-treasure [world treasure-count]
  (loop [world world
         treasure-count treasure-count]
    (if (pos? treasure-count)
      (recur (replace-in-world world
                               (* block-size (inc (rand-int 18)))
                               (* block-size (inc (rand-int 18)))
                               (texture "dollar.jpg")
                               :type :gold)
             (dec treasure-count))
      world)))

(defn create-starting-world
  [width height]
  (-> (apply merge
             ;BODY
             (for [x (range 1 (dec width))
                   y (range 1 (dec height))]
               (assoc (texture "period.jpg")
                 :x (* x block-size) :y (* y block-size)
                 :type :empty))
             ;BOTTOM ROW
             (for [x (range 1 19)]
               (assoc (texture "percent.jpg")
                 :x (* x block-size) :y 0
                 :type :wall))
             ;TOP ROW
             (for [x (range 1 19)]
               (assoc (texture "percent.jpg")
                 :x (* x block-size) :y (* (dec height) block-size)
                 :type :wall))
             ;LEFT COL
             (for [y (range 1 19)]
               (assoc (texture "percent.jpg")
                 :x 0 :y (* y block-size)
                 :type :wall))
             ;RIGHT COL
             (for [y (range 1 19)]
               (assoc (texture "percent.jpg")
                 :x (* (dec width) block-size) :y (* y block-size)
                 :type :wall)))
      (init-treasure 50)
      (replace-in-world (* 10 block-size) (* 10 block-size)
                        (texture "at.jpg")
                        :type :player)))

(defn get-player
  [entities]
  (first
    (filter (fn [e] (= (:type e) :player))
            entities)))

(defscreen main-screen
  :on-show
  (fn [screen _]
    (update! screen :renderer (stage) :camera (orthographic))
    (conj
      (create-starting-world (world-sizes 0) (world-sizes 1))
      (assoc (label (str "Moves Left: " @moves-left)
                    (color :white)
                    :set-y (* (inc (world-sizes 1)) block-size))
        :id :moves-left)
      (assoc (label (str "Score: " @score)
                    (color :white)
                    :set-y (* (inc (world-sizes 1)) block-size)
                    :set-x (* 6 block-size))
        :id :score)))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (label! (first
              (filter
                (fn [e]
                  (= :moves-left (:id e)))
                entities))
            :set-text (str "Moves Left: " @moves-left))
    (label! (first
              (filter
                (fn [e]
                  (= :score (:id e)))
                entities))
            :set-text (str "Score: " @score))
    (render! screen entities))

  :on-resize
  (fn [screen _]
    (height! screen 600))

  :on-key-down
  (fn [screen entities]
    (cond
      (not (pos? @moves-left))
      nil

      (= (:key screen) (key-code :dpad-up))
      (move-in entities (get-player entities) :up)

      (= (:key screen) (key-code :dpad-down))
      (move-in entities (get-player entities) :down)

      (= (:key screen) (key-code :dpad-right))
      (move-in entities (get-player entities) :right)

      (= (:key screen) (key-code :dpad-left))
      (move-in entities (get-player entities) :left))))

(defgame clj-rouje-like
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
