(ns rouje-like.player
  (:require [rouje-like.entity :as rj.e]))

(defn move-player!
  [system direction]
  ;; TODO: REFACTOR SCORE & MOVES-LEFT TO THEIR RESP NS'S,
  ;; TODO: FIRST TEST MOVING THEM TO A NEW FUNCTION
  (let [e-score (first (rj.e/all-e system :score))
        c-score (rj.e/get-c system e-score :score)
        score (:score c-score) ;atom!

        e-moves-left (first (rj.e/all-e system :moves-left))
        c-moves-left (rj.e/get-c system e-moves-left :moves-left)
        moves-left (:moves-left c-moves-left) ;atom!

        e-player (first (rj.e/all-e system :player))
        c-player (rj.e/get-c system e-player :player)
        board (:tiles c-player) ;atom!

        position (rj.e/get-c system e-player :position)
        x-pos (:x position) ;atom!
        y-pos (:y position) ;atom!

        target-coords (case direction
                        :up    [     @x-pos (inc @y-pos)]
                        :down  [     @x-pos (dec @y-pos)]
                        :left  [(dec @x-pos)     @y-pos]
                        :right [(inc @x-pos)     @y-pos])
        target (get-in @board target-coords)]
    (if (pos? @moves-left)
      (case (:type target)
        :wall    system

        :gold    (do
                   (swap! score inc)
                   (println "score: " @score)
                   (swap! moves-left dec)
                   (println "moves-left: " @moves-left)
                   (swap! board (fn [old]
                                  (-> old
                                      (update-in [@x-pos @y-pos]
                                                 (fn [x] (assoc x :type :empty)))
                                      (update-in target-coords
                                                 (fn [x] (assoc x :type :player))))))
                   (reset! x-pos (target-coords 0))
                   (reset! y-pos (target-coords 1))
                   system)

        (do
          (swap! moves-left dec)
          (println "moves-left: " @moves-left)
          (swap! board (fn [old]
                         (-> old
                             (update-in [@x-pos @y-pos]
                                        (fn [x] (assoc x :type :empty)))
                             (update-in target-coords
                                        (fn [x] (assoc x :type :player))))))
          (reset! x-pos (target-coords 0))
          (reset! y-pos (target-coords 1))
          system))))
  system)

(defn process-one-game-tick
  [system _]
  #_(process player#moves-left)
  #_(process player#score)
  system)