(ns rouje-like.input
  (:require [play-clj.core :as play]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.w]
            [rouje-like.utils :refer [?]]
            [rouje-like.utils :as rj.u]
            [rouje-like.player :as rj.pl]
            [rouje-like.components :as rj.c :refer [tick]]
            [rouje-like.messaging :as rj.msg]
            [rouje-like.inventory :as rj.inv]
            [rouje-like.items :as rj.item]
            [rouje-like.destructible :as rj.d]
            [rouje-like.config :as rj.cfg]
            [rouje-like.save-game :as rj.save]
            [rouje-like.magic :as rj.mag]

            [clojure.string :as s]
            [brute.entity]))

#_(in-ns 'rouje-like.input)
#_(use 'rouje-like.input :reload)

(def input-manager (atom {}))

(defn reset-input-manager!
  []
  (reset! input-manager {}))

(defn set-input-manager!
  [mode]
  (swap! input-manager
         assoc mode true))

(defn tick-entities
  [system]
  {:pre [(not (nil? system))]}
  (as-> (let [entities (rj.e/all-e-with-c system :tickable)
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
                  system entities)) system
    (let [energetic-entities (rj.e/all-e-with-c system :energy)]
      (reduce (fn [system entity]
                (rj.e/upd-c system entity :energy
                            (fn [{:keys [default-energy]
                                  :or {default-energy 1}
                                  :as c-energy}]
                              (update-in c-energy [:energy]
                                         #(if (< % default-energy)
                                            default-energy %)))))
              system energetic-entities))))

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
                                                      (keyword c))))))
                     "save"  (fn [system save-name]
                               (rj.save/save-game system save-name))
                     "load"  (fn [system save-name]
                               (rj.save/load-game system save-name))}
        cmd&arg (first (partition 2 (s/split cmds #" ")))
        action (cmd->action (first cmd&arg) (fn [s _] identity s))
        arg (second cmd&arg)]
    (action system arg)))

(def keycode->cmdl-action
  (let
    [k->cmdl-fn (fn [k] (fn [system] (swap! rj.u/cmdl-buffer str (name k)) system))
     key-codes (range 29 55) ;[a-z]
     alphabet [:a :b :c :d :e :f :g :h :i :j :k :l :m
               :n :o :p :q :r :s :t :u :v :w :x :y :z]]
    (merge {(play/key-code :escape)    (fn [system]
                                         (reset-input-manager!)
                                         (reset! rj.u/cmdl-buffer "")
                                         system)
            (play/key-code :backspace) (fn [system]
                                         (swap! rj.u/cmdl-buffer #(apply str (drop-last 1 %)))
                                         system)
            (play/key-code :enter)     (fn [system]
                                         (let [cmds @rj.u/cmdl-buffer]
                                           (reset-input-manager!)
                                           (reset! rj.u/cmdl-buffer "")
                                           (cmds->action system cmds)))
            (play/key-code :space)     (fn [system] (swap! rj.u/cmdl-buffer str " ") system)}
           (zipmap key-codes (map k->cmdl-fn alphabet)))))

(def keycode->action
  {(play/key-code :semicolon)     (fn [system]
                                    (set-input-manager! :cmdl-mode?)
                                    system)
   (play/key-code :F)             (fn [system]
                                    (rj.e/upd-c system (first (rj.e/all-e-with-c system :player))
                                                :player (fn [c-player]
                                                          (update-in c-player [:fog-of-war?]
                                                                     (fn [prev]
                                                                       (not prev))))))
   (play/key-code :E)             (fn [system]
                                    (let [e-player (first (rj.e/all-e-with-c system :player))]
                                      (rj.inv/equip-slot-item system e-player)))
   (play/key-code :num-1)         (fn [system]
                                    (reset-input-manager!)
                                    (set-input-manager! :spell-mode?)
                                    system)

   (play/key-code :enter)         (fn [system]
                                    (tick-entities system))
   (play/key-code :I)             (fn [system]
                                    (reset-input-manager!)
                                    (set-input-manager! :inspect-mode?)
                                    system)
   (play/key-code :H)             (fn [system]
                                    (-> (rj.item/use-hp-potion system (first (rj.e/all-e-with-c system :player)))
                                        (tick-entities)))
   (play/key-code :M)             (fn [system]
                                    (-> (rj.item/use-mp-potion system (first (rj.e/all-e-with-c system :player)))
                                        (tick-entities)))})

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

(defn inspect
  [system direction]
  (let [e-player (first (rj.e/all-e-with-c system :player))
        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        {:keys [x y z]} (rj.e/get-c-on-e system e-player :position)
        player-pos [x y]

        level (nth levels z)
        target-pos (rj.u/coords+offset player-pos (rj.u/direction->offset direction))
        target-tile (get-in level target-pos)

        {target-id :id} (rj.u/tile->top-entity target-tile)]
    (let [{:keys [msg]
           :or {msg "found nothing to inspect"}}
          (rj.e/get-c-on-e system target-id :inspectable)]
        (rj.msg/add-msg system :static msg))))

(defn process-keyboard-input
  [system keycode]
  (let [direction (keycode->direction keycode)
        action (keycode->action keycode)]
    (cond
      (:cmdl-mode? @input-manager)
      (if-let [cmdl-action (keycode->cmdl-action keycode)]
        (cmdl-action system)
        system)

      action
      (action system)

      (:spell-mode? @input-manager)
      (as-> system system
        (do (reset-input-manager!) system)
        (rj.mag/cast-spell system direction)
        (tick-entities system))

      (:inspect-mode? @input-manager)
      (as-> system system
        (do (reset-input-manager!) system)
        (inspect system direction)
        (tick-entities system))

      :else
      (let [e-this (first (rj.e/all-e-with-c system :player))
            {:keys [energy]} (rj.e/get-c-on-e system e-this :energy)]
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
