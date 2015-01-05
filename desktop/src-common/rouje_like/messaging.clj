(ns rouje-like.messaging
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [brute.entity :as br.e]

            [rouje-like.rendering :as rj.r]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c :refer [tick]]
            [rouje-like.utils :as rj.u]))

(defn put-msg-in
  [system k-buffer msg]
  (let [e-relay (first (rj.e/all-e-with-c system :relay))
        message {:message msg
                 :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                             c-counter (rj.e/get-c-on-e system e-counter :counter)]
                         (:turn c-counter))}]
    (rj.e/upd-c system e-relay :relay
                (fn [c-relay]
                  (update-in c-relay [k-buffer]
                             conj message)))))

(defn add-msg
  [system msg]
  (let [e-relay (first (rj.e/all-e-with-c system :relay))]
    (put-msg-in system :background msg)))

(defn add-msg!
  [system msg]
  (let [e-relay (first (rj.e/all-e-with-c system :relay))
        c-this (rj.e/get-c-on-e system e-relay :renderable)
        render-fn (:render-fn c-this)]
    (render-fn nil e-relay nil system)
    (put-msg-in system :immediate msg)))

(defn clear-immediate
  [c-relay]
  (assoc c-relay :immediate []))

(defn clear-background
  [system c-relay]
  (update-in
    c-relay [:background]
    (fn [background-buffer]
      (let [e-counter (first (rj.e/all-e-with-c system :counter))
            c-counter (rj.e/get-c-on-e system e-counter :counter)
            current-turn (:turn c-counter)]
        (remove #(< (:turn %)
                    (dec current-turn))
                background-buffer)))))

(defn clear-messages!
  [system]
  (let [e-relay (first (rj.e/all-e-with-c system :relay))
        {:keys [render-fn]} (rj.e/get-c-on-e system e-relay :renderable)]
    (as-> system system
      (rj.e/upd-c system e-relay :relay
                  #(->> %
                        clear-immediate
                        (clear-background system)))
      (do (render-fn nil e-relay nil system) system))))

(defn process-input-tick
  [_ e-this system]
  (rj.e/upd-c system e-this :relay
              #(->> %
                    (clear-background system)
                    clear-immediate)))

(defn init-messaging
  [system]
  (let [e-this (br.e/create-entity)
        e-counter (br.e/create-entity)
        inc-counter (fn [_ e-this system]
                      (rj.e/upd-c system e-this :counter
                                  #(update-in % [:turn] inc)))]
    (as-> system system
      (rj.e/system<<components
        system e-this
        [[:relay {:background []
                  :immediate []}]
         [:tickable {:tick-fn process-input-tick
                     :extra-tick-fn nil
                     :pri -1}]
         [:renderable {:render-fn rj.r/render-messages
                       :args {}}]])
      (rj.e/system<<components
        system e-counter
        [[:counter {:turn 1}]
         [:tickable {:pri -2
                     :extra-tick-fn nil
                     :tick-fn inc-counter}]]))))
