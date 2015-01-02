(ns rouje-like.messaging
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [brute.entity :as br.e]

            [rouje-like.rendering :as rj.r]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c :refer [tick]]
            [rouje-like.utils :as rj.u]))

(defn add-msg
  [system k-buffer msg]
  (let [e-relay (first (rj.e/all-e-with-c system :relay))]
    (as-> system system
      (rj.e/upd-c system e-relay :relay
                  (fn [c-relay]
                    (update-in c-relay [k-buffer]
                               conj {:message msg
                                     :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                                 c-counter (rj.e/get-c-on-e system e-counter :counter)]
                                             (:turn c-counter))})))
      (if (= k-buffer :blocking)
        (let [c-this (rj.e/get-c-on-e system e-relay :renderable)
              render-fn (:render-fn c-this)]
          (render-fn nil e-relay nil system)
          system)
        system))))

(defn process-input-tick
  [_ e-this system]
  (rj.e/upd-c system e-this :relay
              (fn [c-relay]
                (-> c-relay
                    (update-in [:static]
                               (fn [static-buffer]
                                 (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                       c-counter (rj.e/get-c-on-e system e-counter :counter)
                                       current-turn (:turn c-counter)]
                                   (remove #(< (:turn %) (dec current-turn))
                                           static-buffer))))
                    (assoc :blocking [])))))

(defn init-relay
  [system]
  (let [e-this (br.e/create-entity)
        e-counter (br.e/create-entity)]
    (as-> (rj.e/system<<components
            system e-this
            [[:relay {:static []
                      :blocking []}]
             [:tickable {:tick-fn process-input-tick
                         :extra-tick-fn nil
                         :pri -1}]
             [:renderable {:render-fn rj.r/render-messages
                           :args {}}]]) system
      (rj.e/system<<components
        system e-counter
        [[:counter {:turn 1}]
         [:tickable {:pri -2
                     :extra-tick-fn nil
                     :tick-fn (fn [_ e-this system]
                                (rj.e/upd-c system e-this :counter
                                            (fn [c-counter]
                                              (update-in c-counter [:turn]
                                                         inc))))}]]))))
