(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.w]
            [rouje-like.player :as rj.pl]
            [rouje-like.components :as rj.c :refer [tick]]
            [brute.entity]))


(defn tick-entities
  [system]
  {:pre [(not (nil? system))]} 
  (let [entities (rj.e/all-e-with-c system :tickable)
        e-player (first (rj.e/all-e-with-c system :player))
        c-position (rj.e/get-c-on-e system e-player :position)
        z (:z c-position)
        entities (filter #(if-let [c-position (rj.e/get-c-on-e system % :position)]
                            (= z (:z c-position))
                            true) ;(This is for the relay and the counter) 
                         entities)
        entities (reverse (sort-by :pri entities))#_(SORT in decreasing order)]
    (reduce (fn [system entity]
              (let [c-tickable (rj.e/get-c-on-e system entity :tickable)]
                (tick c-tickable entity system)))
            system entities)))

(defn inc-level
  [system]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        c-mobile (rj.e/get-c-on-e system e-player :mobile)
        c-position (rj.e/get-c-on-e system e-player :position)
        z (:z c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        target-tile (get-in levels [(inc z) (:x c-position) (:y c-position)]
                            nil)]
    (if target-tile
      (rj.c/move c-mobile e-player target-tile system)
      (as-> (rj.w/add-level system (inc z)) system
        (let [levels (:levels (rj.e/get-c-on-e system e-world :world))
              new-level (get-in levels [(inc z) (:x c-position) (:y c-position)])] 
          (rj.c/move c-mobile e-player new-level system))))))

(defn dec-level
  [system]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        c-mobile (rj.e/get-c-on-e system e-player :mobile)
        c-position (rj.e/get-c-on-e system e-player :position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        target-tile (get-in levels [(dec (:z c-position)) (:x c-position) (:y c-position)]
                            nil)]
    (if target-tile
      (rj.c/move c-mobile e-player target-tile system)
      system)))

(def keycode->action
  {(play/key-code :F)             (fn [system]
                                    (rj.e/upd-c system (first (rj.e/all-e-with-c system :player))
                                                :player (fn [c-player]
                                                          (update-in c-player [:show-world?]
                                                                     (fn [prev]
                                                                       (not prev))))))
   (play/key-code :enter)         (fn [system]
                                    (tick-entities system))
   (play/key-code :right-bracket) inc-level
   (play/key-code :space)         inc-level

   (play/key-code :left-bracket)  dec-level
   (play/key-code :shift-left)    dec-level})

(def keycode->direction
  {(play/key-code :W)          :up
   (play/key-code :dpad-up)    :up
   (play/key-code :K)          :up

   (play/key-code :S)          :down
   (play/key-code :dpad-down)  :down
   (play/key-code :J)          :down

   (play/key-code :A)          :left
   (play/key-code :dpad-left)  :left
   (play/key-code :H)          :left

   (play/key-code :D)          :right
   (play/key-code :dpad-right) :right
   (play/key-code :L)          :right})

(defn process-keyboard-input
  [system key-code]
  (let [action (keycode->action key-code)]
    (if (not (nil? action))
      (action system)

      (let [this (first (rj.e/all-e-with-c system :player))
            direction (keycode->direction key-code)]
        (if (not (nil? direction)) 
          (-> system
              (rj.pl/process-input-tick direction)
              (tick-entities))
          system)))))

(defn process-fling-input
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
