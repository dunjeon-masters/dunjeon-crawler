(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity :as rj.e]
            [rouje-like.world  :as rj.w]
            [rouje-like.player :as rj.pl]))

(defn process-keyboard-input
  [system key-code]
  (cond
    (= key-code (play/key-code :dpad-up))
    (rj.pl/move-player! system :up)

    (= key-code (play/key-code :dpad-down))
    (rj.pl/move-player! system :down)

    (= key-code (play/key-code :dpad-left))
    (rj.pl/move-player! system :left)

    (= key-code (play/key-code :dpad-right))
    (rj.pl/move-player! system :right)

    (= key-code (play/key-code :F))
    (swap! (:show-world? (rj.e/get-c-on-e system
                                             (first (rj.e/all-e-with-c system :player))
                                             :player))
           (fn [prev] (not prev)))))

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
