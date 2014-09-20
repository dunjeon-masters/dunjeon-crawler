(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity :as rj.e]
            [rouje-like.world  :as rj.w]
            [rouje-like.player :as rj.pl]))

(defn process-keyboard-input
  [system key-code]
  (cond
    (= key-code (play/key-code :dpad-up))
    (rj.pl/process-input-tick! system :up)

    (= key-code (play/key-code :dpad-down))
    (rj.pl/process-input-tick! system :down)

    (= key-code (play/key-code :dpad-left))
    (rj.pl/process-input-tick! system :left)

    (= key-code (play/key-code :dpad-right))
    (rj.pl/process-input-tick! system :right)

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
      (rj.pl/process-input-tick! system :down)
      (rj.pl/process-input-tick! system :up))
    (if (pos? x-vel)
      (rj.pl/process-input-tick! system :right)
      (rj.pl/process-input-tick! system :left))))
