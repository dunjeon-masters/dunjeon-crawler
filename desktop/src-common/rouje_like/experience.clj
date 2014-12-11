(ns rouje-like.experience
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :refer [can-attack? attack]]
            [rouje-like.config :as rj.cfg]
            [rouje-like.messaging :as rj.msg]))

(defn level->exp
  "[int level] gets the minimum amount of experience needed to be LEVEL"
  [level]
  (* level (:exp rj.cfg/level-exp)))

(defn wrand
  "[int vector slices] returns a random index chosen with the weights provided in SLICES"
  [slices]
  (let [total (reduce + slices)
        r (rand total)]
    (loop [i 0 sum 0]
      (if (< r (+ (slices i) sum))
        i
        (recur (inc i) (+ (slices i) sum))))))

(defn level-up-stats
  ([system e-this]
   (let [stat-to-level-up (get (conj (vec (keys rj.cfg/player-stats)) :all)
                               (wrand [3 3 3 3 1]))
         c-experience (rj.e/get-c-on-e system e-this :experience)
         c-magic (rj.e/get-c-on-e system e-this :magic)
         c-class (rj.e/get-c-on-e system e-this :class)
         player-class (:class c-class)
         spells (:spells c-magic)
         level (:level c-experience)]

     (as-> system system
       (if (zero? (mod level 5))
         (if (zero? (count spells))
           (let [cfg-spell-player-class (rj.cfg/class->spell player-class)
                 spell (rand-nth cfg-spell-player-class)
                 cfg-spell-effect (spell rj.cfg/spell-effects)]
             (rj.e/upd-c system e-this :magic
                         (fn [c-magic]
                           (update-in c-magic [:spells]
                                      (fn [spells]
                                        (vec (conj spells
                                                   {:name spell
                                                    :distance (:distance cfg-spell-effect)
                                                    :value (:value cfg-spell-effect)
                                                    :type (:type cfg-spell-effect)
                                                    :atk-reduction (:atk-reduction cfg-spell-effect)})))))))

           ;upgrade spell
           (let [spell-to-upgrade (:name (rand-nth spells))]
             (rj.e/upd-c system e-this :magic
                         (fn [c-magic]
                           (update-in c-magic [:spells]
                                      (fn [spells]
                                        (map #(if (= spell-to-upgrade
                                                     (:name %))
                                                (update-in % [:value]
                                                           + 2)
                                                %)
                                             spells)))))))
         system)

       (if (= stat-to-level-up :all)
         (-> system
             (level-up-stats e-this :max-hp)
             (level-up-stats e-this :atk)
             (level-up-stats e-this :def)
             (level-up-stats e-this :max-mp))
         (level-up-stats system e-this stat-to-level-up)))))

  ([system e-this stat-to-level-up]
   (let [comp-to-level-up (rj.cfg/stat->comp stat-to-level-up)]
     (rj.e/upd-c system e-this comp-to-level-up
                 (fn [c-comp]
                   (update-in c-comp [stat-to-level-up] (partial + (rj.cfg/stat->pointinc stat-to-level-up))))))))

(defn level-up
  [e-this system]
  (let [c-this (rj.e/get-c-on-e system e-this :experience)
        level (:level c-this)
        experience (:experience c-this)]

    ;Does not handle if player should level up multiple times in one turn
    ;If player needs 3 exp per level and gains 9 from killing a boss, it will
    ;only level up once. Perhaps use a loop?
    (if (> experience (level->exp level))
      (-> system
          (rj.e/upd-c e-this :experience
                  (fn [c-level]
                    (update-in c-level [:level] inc)))
          (rj.msg/add-msg :static
                          (format "You leveled up! You are now level %d"
                                  (inc level)))
          (level-up-stats e-this))
      system)))

