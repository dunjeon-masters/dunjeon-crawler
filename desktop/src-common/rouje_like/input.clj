(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.w]
            [rouje-like.utils :as rj.u]
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
        entities (reverse (sort-by (fn [e]
                                     (:pri (rj.e/get-c-on-e system e :tickable))) 
                                   entities)) #_(SORT in decreasing order)]
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

;Temporary, will eventually be used to execute cmdl actions
(defn cmd-action
  [cmd]
  (println cmd))

;MEGA HACK - Thank vim vim <C-a> & macros
(def keycode->cli-action
  {(play/key-code :escape) (fn [system]
                             (reset! rj.u/cli? false)
                             (reset! rj.u/cli "")
                             system)
   (play/key-code :backspace) (fn [system]
                                (swap! rj.u/cli #(apply str (drop-last 1 %)))
                                system)
   (play/key-code :enter) (fn [system]
                            (cmd-action @rj.u/cli)
                            (reset! rj.u/cli? false)
                            (reset! rj.u/cli "")
                            system)
   (play/key-code :space) (fn [system] (swap! rj.u/cli str " ") system)
   (play/key-code :a) (fn [system] (swap! rj.u/cli str "a") system)
   (play/key-code :b) (fn [system] (swap! rj.u/cli str "b") system)
   (play/key-code :c) (fn [system] (swap! rj.u/cli str "c") system)
   (play/key-code :d) (fn [system] (swap! rj.u/cli str "d") system)
   (play/key-code :e) (fn [system] (swap! rj.u/cli str "e") system)
   (play/key-code :f) (fn [system] (swap! rj.u/cli str "f") system)
   (play/key-code :g) (fn [system] (swap! rj.u/cli str "g") system)
   (play/key-code :h) (fn [system] (swap! rj.u/cli str "h") system)
   (play/key-code :i) (fn [system] (swap! rj.u/cli str "i") system)
   (play/key-code :j) (fn [system] (swap! rj.u/cli str "j") system)
   (play/key-code :k) (fn [system] (swap! rj.u/cli str "k") system)
   (play/key-code :l) (fn [system] (swap! rj.u/cli str "l") system)
   (play/key-code :m) (fn [system] (swap! rj.u/cli str "m") system)
   (play/key-code :n) (fn [system] (swap! rj.u/cli str "n") system)
   (play/key-code :o) (fn [system] (swap! rj.u/cli str "o") system)
   (play/key-code :p) (fn [system] (swap! rj.u/cli str "p") system)
   (play/key-code :q) (fn [system] (swap! rj.u/cli str "q") system)
   (play/key-code :r) (fn [system] (swap! rj.u/cli str "r") system)
   (play/key-code :s) (fn [system] (swap! rj.u/cli str "s") system)
   (play/key-code :t) (fn [system] (swap! rj.u/cli str "t") system)
   (play/key-code :u) (fn [system] (swap! rj.u/cli str "u") system)
   (play/key-code :v) (fn [system] (swap! rj.u/cli str "v") system)
   (play/key-code :w) (fn [system] (swap! rj.u/cli str "w") system)
   (play/key-code :x) (fn [system] (swap! rj.u/cli str "x") system)
   (play/key-code :y) (fn [system] (swap! rj.u/cli str "y") system)
   (play/key-code :z) (fn [system] (swap! rj.u/cli str "z") system)
   (play/key-code :0) (fn [system] (swap! rj.u/cli str "0") system)
   (play/key-code :1) (fn [system] (swap! rj.u/cli str "1") system)
   (play/key-code :2) (fn [system] (swap! rj.u/cli str "2") system)
   (play/key-code :3) (fn [system] (swap! rj.u/cli str "3") system)
   (play/key-code :4) (fn [system] (swap! rj.u/cli str "4") system)
   (play/key-code :5) (fn [system] (swap! rj.u/cli str "5") system)
   (play/key-code :6) (fn [system] (swap! rj.u/cli str "6") system)
   (play/key-code :7) (fn [system] (swap! rj.u/cli str "7") system)
   (play/key-code :8) (fn [system] (swap! rj.u/cli str "8") system)
   (play/key-code :9) (fn [system] (swap! rj.u/cli str "9") system)})

(def keycode->action
  {(play/key-code :semicolon)     (fn [system]
                                    (reset! rj.u/cli? true)
                                    system) 
   (play/key-code :F)             (fn [system]
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
  [system keycode]
  (if @rj.u/cli? 
    (let [cli-action (keycode->cli-action keycode)]
          (if (not (nil? cli-action))
            (cli-action system)
            system))
    (let [action (keycode->action keycode)]
      (if (not (nil? action))
        (action system)

        (let [this (first (rj.e/all-e-with-c system :player))
              direction (keycode->direction keycode)]
          (if (not (nil? direction)) 
            (-> system
                (rj.pl/process-input-tick direction)
                (tick-entities))
            system))))))

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
