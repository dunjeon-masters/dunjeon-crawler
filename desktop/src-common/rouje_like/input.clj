(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.w]
            [rouje-like.player :as rj.pl]
            [rouje-like.components :as rj.c :refer [tick]]))


(defn tick-entities
  [system]
  (let [entities (rj.e/all-e-with-c system :tickable)]
    (reduce (fn [system entity]
              (let [c-tickable (rj.e/get-c-on-e system entity :tickable)]
                (tick c-tickable entity system)))
            system entities)))

(def keycode->action
  {(play/key-code :F)      (fn [system]
                             (rj.e/upd-c system (first (rj.e/all-e-with-c system :player))
                                         :player (fn [c-player]
                                                   (update-in c-player [:show-world?]
                                                              (fn [prev]
                                                                (not prev))))))
   (play/key-code :equals) (fn [system]
                             (rj.e/upd-c system (first (rj.e/all-e-with-c system :player) )
                                         :moves-left (fn [c-moves-left]
                                                       (update-in c-moves-left [:moves-left]
                                                                  (fn [moves-left]
                                                                    (+ 25 moves-left))))))
   (play/key-code :space)  (fn [system]
                             (tick-entities system))})

(def keycode->direction
  {(play/key-code :W)          :up
   (play/key-code :dpad-up)    :up

   (play/key-code :S)          :down
   (play/key-code :dpad-down)  :down

   (play/key-code :A)          :left
   (play/key-code :dpad-left)  :left

   (play/key-code :D)          :right
   (play/key-code :dpad-right) :right})

(defn process-keyboard-input!
  [system key-code]
  (let [action (keycode->action key-code)]
    (if (not (nil? action))
      (action system)

      (let [this (first (rj.e/all-e-with-c system :player))
            c-moves-left (rj.e/get-c-on-e system this :moves-left)
            moves-left (:moves-left c-moves-left)
            direction (keycode->direction key-code)]
        (if (and (pos? moves-left)
                 (not (nil? direction)))
          (-> system
              (rj.pl/process-input-tick direction)
              (tick-entities))
          system)))))

(defn process-fling-input!
  [system x-vel y-vel]
  (-> system
      (as-> system (if (< (* x-vel x-vel)
                          (* y-vel y-vel))
                     (if (pos? y-vel)
                       (rj.pl/process-input-tick system :down)
                       (rj.pl/process-input-tick system :up))
                     (if (pos? x-vel)
                       (rj.pl/process-input-tick system :right)
                       (rj.pl/process-input-tick system :left))))
      (tick-entities)))
