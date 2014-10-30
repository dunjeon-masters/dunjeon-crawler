(ns rouje-like.experience
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :refer [can-attack? attack]]
            [rouje-like.config :as rj.cfg]
            [rouje-like.messaging :as rj.msg]))

(defn level->exp
  [level]
  (* level (:exp rj.cfg/level-exp)))

(defn wrand
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
                              (wrand [3 3 3 1]))]
    (if (= stat-to-level-up :all)
      (-> system
          (level-up-stats e-this :hp)
          (level-up-stats e-this :atk)
          (level-up-stats e-this :def))
      (level-up-stats system e-this stat-to-level-up))))
  ([system e-this stat-to-level-up]
   (println stat-to-level-up)
   (let [comp-to-level-up (rj.cfg/stat->comp stat-to-level-up)]
     (rj.e/upd-c system e-this comp-to-level-up
                 (fn [c-comp]
                   (update-in c-comp [stat-to-level-up] (partial + (rj.cfg/stat->pointinc stat-to-level-up))))))))

(defn level-up
  [e-this system]
  (let [c-this (rj.e/get-c-on-e system e-this :experience)
        level (:level c-this)
        experience (:experience c-this)]

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