(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.w]
            [rouje-like.utils :as rj.u]
            [rouje-like.player :as rj.pl]
            [rouje-like.components :as rj.c :refer [tick]]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.inventory :as rj.inv]
            [rouje-like.items :as rj.item]
            [clojure.string :as s]
            [brute.entity]
            [rouje-like.magic :as rj.mag]))

#_(in-ns 'rouje-like.input)
#_(use 'rouje-like.input :reload)

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

;Temporary, will eventually be used to execute cmdl actions
(defn cmds->action
  [system cmds]
  (let [cmd->action {"race" (fn [system r]
                              (let [e-player (first (rj.e/all-e-with-c system :player))]
                                (rj.e/upd-c system e-player :race
                                            (fn [c-race]
                                              (assoc c-race :race
                                                     (keyword r))))))
                     "class" (fn [system c]
                               (let [e-player (first (rj.e/all-e-with-c system :player))]
                                 (rj.e/upd-c system e-player :class
                                             (fn [c-class]
                                               (assoc c-class :class
                                                      (keyword c))))))}
        cmd&arg (first (partition 2 (s/split cmds #" ")))
        action (cmd->action (first cmd&arg) (fn [s _] identity s))
        arg (second cmd&arg)]
    (action system arg)))

(def keycode->cli-action
  (let
    [k->cli-fn (fn [k] (fn [system] (swap! rj.u/cli str (name k)) system))
     key-codes (range 29 55) ;[a-z]
     alphabet [:a :b :c :d :e :f :g :h :i :j :k :l :m
               :n :o :p :q :r :s :t :u :v :w :x :y :z]]
    (merge {(play/key-code :escape)    (fn [system]
                                         (reset! rj.u/cli? false)
                                         (reset! rj.u/cli "")
                                         system)
            (play/key-code :backspace) (fn [system]
                                         (swap! rj.u/cli #(apply str (drop-last 1 %)))
                                         system)
            (play/key-code :enter)     (fn [system]
                                         (let [cli @rj.u/cli]
                                           (reset! rj.u/cli? false)
                                           (reset! rj.u/cli "")
                                           (cmds->action system cli)))
            (play/key-code :space)     (fn [system] (swap! rj.u/cli str " ") system)}
           (zipmap key-codes (map k->cli-fn alphabet)))))

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
   (play/key-code :E)             (fn [system]
                                    (let [e-player (first (rj.e/all-e-with-c system :player))]
                                      (rj.inv/equip-slot-item system e-player)))
   (play/key-code :num-1)             (fn [system]
                                    (let [e-player (first (rj.e/all-e-with-c system :player))
                                          c-magic (rj.e/get-c-on-e system e-player :magic)
                                          mp (:mp c-magic)]
                                      (as-> (rj.mag/use-fireball system e-player :right) system
                                            (if (pos? mp)
                                              (tick-entities system)
                                              system))))
   (play/key-code :enter)         (fn [system]
                                    (tick-entities system))
   (play/key-code :H)             (fn [system]
                                    (rj.item/use-hp-potion system (first (rj.e/all-e-with-c system :player))))
   (play/key-code :M)             (fn [system]
                                    (rj.item/use-mp-potion system (first (rj.e/all-e-with-c system :player))))})

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
  (prn "foo")
  (if @rj.u/cli?
    (let [cli-action (keycode->cli-action keycode)]
          (if (not (nil? cli-action))
            (cli-action system)
            system))
    (let [action (keycode->action keycode)]
      (if (not (nil? action))
        (action system)

        (let [e-this (first (rj.e/all-e-with-c system :player))
              c-energy (rj.e/get-c-on-e system e-this :energy)
              energy (:energy c-energy)
              direction (keycode->direction keycode)]
          (if (not (nil? direction))
            (as-> system system
              (if (pos? energy)
                (rj.pl/process-input-tick system direction)
                (if-let [c-broadcaster (rj.e/get-c-on-e system e-this :broadcaster)]
                  (rj.msg/add-msg system :static
                                  (format "%s was paralyzed, and couldn't move this turn"
                                          ((:name-fn c-broadcaster) system e-this)))
                  system))
              (if (>= 1 (:energy (rj.e/get-c-on-e system e-this :energy)))
                (tick-entities system)
                system)
              (let [energetic-entities (rj.e/all-e-with-c system :energy)]
                (reduce (fn [system entity]
                          (rj.e/upd-c system entity :energy
                                      (fn [c-energy]
                                        (update-in c-energy [:energy]
                                                   (fn [energy]
                                                     (if (< energy 1)
                                                       (inc energy)
                                                       energy))))))
                        system energetic-entities)))
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

