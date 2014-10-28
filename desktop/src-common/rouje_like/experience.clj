(ns rouje-like.experience
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.world :as rj.wr]
            [rouje-like.components :refer [can-attack? attack]]
            [rouje-like.config :as rj.cfg]))

(defmacro debug
  [var]
  (let [var# var]
    `(println (str '~var# ": " ~var#))))

(defn level->exp
  [level]
  (* level (:exp rj.cfg/level-exp)))

(defn level-up
  [e-this system]
  (let [c-this (rj.e/get-c-on-e system e-this :experience)
        level (:level c-this)
        experience (:experience c-this)
        e-relay (first (rj.e/all-e-with-c system :relay))]

    (if (> experience (level->exp level))
      (-> system
          (rj.e/upd-c e-this :experience
                  (fn [c-level]
                    (update-in c-level [:level] inc)))
          (rj.e/upd-c e-relay :relay
                      (fn [c-relay]
                        (update-in c-relay [:static]
                                   conj {:message (format "You leveled up! You are now level %d"
                                                          (inc level))
                                         :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                                     c-counter (rj.e/get-c-on-e system e-counter :counter)]
                                                 (:turn c-counter))}))))
      system)))