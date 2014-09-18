(ns rouje-like.input
  (:require [play-clj.core :refer :all]

            [rouje-like.player :as rj.pl]))

(defn process-keyboard-input
  [system]
  (cond
    (key-pressed? :dpad-up)
    (rj.pl/move-player! system :up)

    (key-pressed? :dpad-down)
    (rj.pl/move-player! system :down)

    (key-pressed? :dpad-left)
    (rj.pl/move-player! system :left)

    (key-pressed? :dpad-right)
    (rj.pl/move-player! system :right)

    :else system))
(defn process-fling-input
  [system x-vel y-vel]
  (if (< (* x-vel x-vel)
         (* y-vel y-vel))
    (if (pos? y-vel)
      (rj.pl/move-player! system :down)
      (rj.pl/move-player! system :up))
    (if (pos? x-vel)
      (rj.pl/move-player! system :right)
      (rj.pl/move-player! system :left))))
