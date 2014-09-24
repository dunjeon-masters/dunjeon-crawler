(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer [texture texture!]]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]))

(defn take-damage
  [system this damage _]
  (let [c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (if (pos? (- hp damage))
      (-> system
          (rj.e/upd-c this :destructible
                      (fn [c-destructible]
                        (update-in c-destructible [:hp] - damage))))
      (-> system
          (rj.e/upd-c e-world :world
                      (fn [c-world]
                        (update-in c-world [:world]
                                   (fn [world]
                                     (update-in world [(:x c-position) (:y c-position)]
                                                (fn [tile]
                                                  (update-in tile [:entities]
                                                             (fn [entities]
                                                               (vec (remove #(#{:player} (:type %))
                                                                            entities))))))))))
          (rj.e/kill-e this)))))

(defn can-attack?
  [_ _ target]
  (#{:lichen :bat} (-> target (:entities)
                       (rj.c/sort-by-pri)
                       (first) (:type))))

(defn attack
  [system this target]
  (let [damage (:atk (rj.e/get-c-on-e system this :attacker))

        e-enemy (:id (-> target (:entities)
                         (rj.c/sort-by-pri)
                         (first)))

        take-damage (:take-damage (rj.e/get-c-on-e system e-enemy :destructible))]
    (take-damage system e-enemy damage this)))

(defn can-dig?
  [_ _ target]
  (#{:wall} (:type (-> target (:entities)
                       (rj.c/sort-by-pri)
                       (first)))))

(defn dig
  [system this target]
  (let [e-world (first (rj.e/all-e-with-c system :world))

        wall->floor (fn [world ks]
                      (update-in world ks
                                 (fn [tile]
                                   (update-in tile [:entities]
                                              (fn [entities]
                                                (remove #(#{:wall} (:type %))
                                                        entities))))))]
    (-> system
        (rj.e/upd-c e-world :world
                    (fn [c-world]
                      (update-in c-world [:world]
                                 wall->floor [(:x target) (:y target)])))
        (rj.e/upd-c this :moves-left
                    (fn [c-moves-left]
                      (update-in c-moves-left [:moves-left] dec))))))

(defn can-move?
  [_ _ target]
  (#{:floor :gold :torch} (:type (-> target (:entities)
                                     (rj.c/sort-by-pri)
                                     (first)))))

(defn move
  [system this target]
  (let [c-sight (rj.e/get-c-on-e system this :sight)
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

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (-> system
        (rj.e/upd-c this :moves-left
                    (fn [c-moves-left]
                      (update-in c-moves-left [:moves-left] dec)))

        (as-> system (case (-> target (:entities)
                               (rj.c/sort-by-pri)
                               (first) (:type))
                       :gold  (-> system
                                  (rj.e/upd-c this :gold
                                              (fn [c-gold]
                                                (update-in c-gold [:gold] inc)))
                                  (rj.e/upd-c this :sight
                                              (fn [c-sight]
                                                (update-in c-sight [:distance] dec-sight))))
                       :torch (-> system
                                  (rj.e/upd-c this :sight
                                              (fn [c-sight]
                                                (update-in c-sight [:distance] inc-sight))))
                       :floor (-> system
                                  (rj.e/upd-c this :sight
                                              (fn [c-sight]
                                                (update-in c-sight [:distance] dec-sight))))
                       system))

        (rj.e/upd-c e-world :world
                    (fn [c-world]
                      (update-in c-world [:world]
                                 (fn [world]
                                   (let [player-pos [(:x c-position) (:y c-position)]
                                         target-pos [(:x target) (:y target)]]
                                     (-> world
                                         (update-in target-pos
                                                    (fn [tile]
                                                      (update-in tile [:entities]
                                                                 (fn [entities]
                                                                   (vec (conj
                                                                          (remove #(#{:gold :torch} (:type %))
                                                                                  entities)
                                                                          (rj.c/map->Entity {:type :player
                                                                                             :id   this})))))))
                                         (update-in player-pos
                                                    (fn [tile]
                                                      (update-in tile [:entities]
                                                                 (fn [entities]
                                                                   (vec (remove #(#{:player} (:type %))
                                                                                entities))))))))))))

        (rj.e/upd-c this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target))
                          (assoc-in [:y] (:y target))))))))

(defn process-input-tick!
  [system direction]
  (let [this (first (rj.e/all-e-with-c system :player))

        c-position (rj.e/get-c-on-e system this :position)
        x-pos (:x c-position)
        y-pos (:y c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)
        target-coords (case direction
                        :up    [     x-pos (inc y-pos)]
                        :down  [     x-pos (dec y-pos)]
                        :left  [(dec x-pos)     y-pos]
                        :right [(inc x-pos)     y-pos])
        target (get-in world target-coords nil)]
    (if (and (not (nil? target)))
      (let [c-mobile   (rj.e/get-c-on-e system this :mobile)
            c-digger   (rj.e/get-c-on-e system this :digger)
            c-attacker (rj.e/get-c-on-e system this :attacker)]
        (cond
          ((:can-move? c-mobile) system this target)
          ((:move c-mobile) system this target)

          ((:can-dig? c-digger) system this target)
          ((:dig c-digger) system this target)

          ((:can-attack? c-attacker) system this target)
          ((:attack c-attacker) system this target)))
      system)))

;;RENDERING FUNCTIONS
(defn render-player-stats
  [system this {:keys [view-port-sizes]}]
  (let [[_ vheight] view-port-sizes

        c-position (rj.e/get-c-on-e system this :position)
        x (:x c-position)
        y (:y c-position)

        c-gold (rj.e/get-c-on-e system this :gold)
        gold (:gold c-gold)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)

        c-destructible (rj.e/get-c-on-e system this :destructible)
        hp (:hp c-destructible)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Gold: [" gold "]" " - " "MovesLeft: [" moves-left "]"
                        " - " "Position: [" x "," y "]" " - " "HP: [" hp "]")
                   (color :green)
                   :set-y (float (* (+ vheight 2) rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [system this args]
  (render-player-stats system this args))
