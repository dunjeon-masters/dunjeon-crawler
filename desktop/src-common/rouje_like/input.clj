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
