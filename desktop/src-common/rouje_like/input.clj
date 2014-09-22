(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity :as rj.e]
            [rouje-like.world :as rj.w]
            [rouje-like.player :as rj.pl]
            [rouje-like.components :as rj.c]))

(defn process-keyboard-input!
  [system key-code]
  (if (cond
        (or (= key-code (play/key-code :equals))
            (= key-code (play/key-code :f))) true
        :else false)
    (cond
      (= key-code (play/key-code :f))
      (rj.e/upd-c system (first (rj.e/all-e-with-c system :player))
                  :player (fn [c-player]
                            (update-in c-player [:show-world?]
                                       (fn [prev]
                                         (not prev)))))
      (= key-code (play/key-code :equals))
      (rj.e/upd-c system (first (rj.e/all-e-with-c system :player))
                  :moves-left (fn [c-moves-left]
                                (update-in c-moves-left [:moves-left]
                                           (fn [moves-left]
                                             (+ 10 moves-left)))))
      :else system)

    (let [this (first (rj.e/all-e-with-c system :player))
          c-moves-left (rj.e/get-c-on-e system this :moves-left)
          moves-left (:moves-left c-moves-left)]
      (if (or (pos? moves-left))
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

              :else system)
            (as-> system
                  (let [entities (rj.e/all-e-with-c system :tickable)]
                    (reduce (fn [system entity]
                              (let [c-tickable (rj.e/get-c-on-e system entity :tickable)]
                                ((:tick-fn c-tickable) system entity (:args c-tickable))))
                            system entities))))
        system))))

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
