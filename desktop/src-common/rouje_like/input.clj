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

(defn reset-input-manager
  []
  (reset! input-manager {}))

(defn set-input-state
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
                                                      (keyword c))))))
                     "save"  (fn [system save-name]
                               (rj.save/save-game system save-name))
                     "load"  (fn [system save-name]
                               (rj.save/load-game system save-name))}
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
                                                          (update-in c-player [:fog-of-war?]
                                                                     (fn [prev]
                                                                       (not prev))))))
   (play/key-code :E)             (fn [system]
                                    (let [e-player (first (rj.e/all-e-with-c system :player))]
                                      (rj.inv/equip-slot-item system e-player)))
   (play/key-code :num-1)         (fn [system]
                                    (reset-input-manager)
                                    (set-input-state :spell-mode)
                                    system)

   (play/key-code :enter)         (fn [system]
                                    (tick-entities system))
   (play/key-code :I)             (fn [system]
                                    (reset-input-manager)
                                    (set-input-state :inspect-mode)
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
        (let [e-this (first (rj.e/all-e-with-c system :player))
              c-energy (rj.e/get-c-on-e system e-this :energy)
              energy (:energy c-energy)
              direction (keycode->direction keycode)]
          (if (not (nil? direction))
            (cond
             ;; are we casting a spell
             (:spell-mode @input-manager)
             (let [e-player (first (rj.e/all-e-with-c system :player))
                   c-magic (rj.e/get-c-on-e system e-player :magic)
                   spells (:spells c-magic)
                   fireball (first (filter #(= (:name %) :fireball) spells))
                   powerattack (first (filter #(= (:name %) :powerattack) spells))
                   pickpocket (first (filter #(= (:name %) :pickpocket) spells))
                   mp (:mp c-magic)]
               (reset-input-manager)
               (cond
                 fireball
                 (if (not (neg? (- mp (:fireball rj.cfg/spell->mp-cost))))
                   (as-> system system
                     (rj.mag/use-fireball system e-player fireball direction)
                     (tick-entities system)
                     (rj.d/apply-effects system e-player))
                   (as-> system system
                     (rj.msg/add-msg system :static "you do not have enough mp to cast fireball")
                     (tick-entities system)))

                 powerattack
                 (if (not (neg? (- mp (:powerattack rj.cfg/spell->mp-cost))))
                   (as-> system system
                     (rj.mag/use-powerattack system e-player powerattack direction)
                     (tick-entities system)
                     (rj.d/apply-effects system e-player))
                   (as-> system system
                     (rj.msg/add-msg system :static "you do not have enough mp to cast power attack")
                     (tick-entities system)))

                 pickpocket
                 (if (not (neg? (- mp (:pickpocket rj.cfg/spell->mp-cost))))
                   (as-> system system
                     (rj.mag/use-pickpocket system e-player pickpocket direction)
                     (tick-entities system)
                     (rj.d/apply-effects system e-player))
                   (as-> system system
                     (rj.msg/add-msg system :static "you do not have enough mp to cast pickpocket")
                     (tick-entities system)))

                 :else
                 (as-> system system
                           (rj.msg/add-msg system :static "you do not have a spell to cast")
                           (tick-entities system))))

            ;; are we inspecting something
             (:inspect-mode @input-manager)
             (let [e-player (first (rj.e/all-e-with-c system :player))
                   e-world (first (rj.e/all-e-with-c system :world))
                   c-world (rj.e/get-c-on-e system e-world :world)
                   levels (:levels c-world)
                   c-pos (rj.e/get-c-on-e system e-player :position)
                   player-pos [(:x c-pos) (:y c-pos)]

                   level (nth levels (:z c-pos))
                   target-pos (rj.u/coords+offset player-pos (rj.u/direction->offset direction))
                   target-entities (rj.u/entities-at-pos level target-pos)

                   inspectable (first (filter rj.u/inspectable? target-entities))]
               (reset-input-manager)
               (if inspectable
                 (let [c-inspectable (rj.e/get-c-on-e system (:id inspectable) :inspectable)
                       msg (:msg c-inspectable)]
                   (as-> system system
                         (rj.msg/add-msg system :static msg)
                         (tick-entities system)))
                 (as-> system system
                       (rj.msg/add-msg system :static "there is nothing to inspect there")
                       (tick-entities system))))

              ;; we must be moving
             :else
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
                     system)))
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
