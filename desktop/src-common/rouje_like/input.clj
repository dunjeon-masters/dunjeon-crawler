(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity :as rj.e]
            [rouje-like.world  :as rj.w]
            [rouje-like.player :as rj.pl]))

(defn process-keyboard-input!
  [system key-code]
  (-> (cond
        (or (= key-code (play/key-code :dpad-up))
            (= key-code (play/key-code :W)))
        (rj.pl/process-input-tick! system :up)

        (or (= key-code (play/key-code :dpad-down))
            (= key-code (play/key-code :S)))
        (rj.pl/process-input-tick! system :down)

        (or (= key-code (play/key-code :dpad-left))
            (= key-code (play/key-code :A)))
        (rj.pl/process-input-tick! system :left)

        (or (= key-code (play/key-code :dpad-right))
            (= key-code (play/key-code :D)))
        (rj.pl/process-input-tick! system :right)

        (= key-code (play/key-code :F))
        (do (swap! (:show-world? (rj.e/get-c-on-e system
                                                  (first (rj.e/all-e-with-c system :player))
                                                  :player))
                   (fn [prev] (not prev)))
            system))
      (as-> system
            (let [entities (rj.e/all-e-with-c system :tickable)]
              ;; TODO: Refactor using reduce
              (loop [system system
                     tickable-entities (rest entities)
                     entity (first entities)]
                (if (not (nil? entity))
                  (let [c-tickable (rj.e/get-c-on-e system entity :tickable)]
                    (recur ((:tick-fn c-tickable) system entity (:args c-tickable))
                           (rest tickable-entities)
                           (first tickable-entities)))
                  system))))))

(defn process-fling-input!
  [system x-vel y-vel]
  (if (< (* x-vel x-vel)
         (* y-vel y-vel))
    (if (pos? y-vel)
      (rj.pl/process-input-tick! system :down)
      (rj.pl/process-input-tick! system :up))
    (if (pos? x-vel)
      (rj.pl/process-input-tick! system :right)
      (rj.pl/process-input-tick! system :left))))
