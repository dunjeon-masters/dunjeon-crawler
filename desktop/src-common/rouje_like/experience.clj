(ns rouje-like.experience
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :refer [can-attack? attack]]))

(defmacro debug
  [var]
  (let [var# var]
    `(println (str '~var# ": " ~var#))))

(defn level->exp
  [level]
  (* level 5))

(defn level-up
  [e-this system]
  (let [c-this (rj.e/get-c-on-e system e-this :experience)
        level (:level c-this)
        experience (:experience c-this)
        _ (debug level)
        _ (debug experience)]

    (if (> experience (level->exp level))
      (rj.e/upd-c system e-this :experience
                  (fn [c-level]
                    (update-in c-level [:level] inc)))
      system)))
