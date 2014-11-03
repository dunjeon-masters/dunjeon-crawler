(ns rouje-like.messaging
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.utils :as rj.u]))

(defn add-msg
  [system k-buffer msg]
  (let [e-relay (first (rj.e/all-e-with-c system :relay))]
    (rj.e/upd-c system e-relay :relay
                (fn [c-relay]
                  (update-in c-relay [k-buffer]
                             conj {:message msg
                                   :turn (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                               c-counter (rj.e/get-c-on-e system e-counter :counter)]
                                           (:turn c-counter))})))))


(defn render-static-messages
  [_ e-this _ system]
  (let [c-relay (rj.e/get-c-on-e system e-this :relay)

        e-counter (first (rj.e/all-e-with-c system :counter))
        c-counter (rj.e/get-c-on-e system e-counter :counter)
        current-turn (:turn c-counter)

        statics (:static c-relay)
        current-messages (filter #(= (:turn %) (dec current-turn))
                                 statics)
        static-messages (mapcat #(str (:message %) ". \n")
                                current-messages)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (apply str (into [] static-messages))
                   (color :green)
                   :set-y (float 0))
            :draw renderer 1.0)
    (.end renderer)))

(defn process-input-tick
  [_ e-this system]
  (rj.e/upd-c system e-this :relay
              (fn [c-relay]
                (update-in c-relay [:static]
                           (fn [static-buffer]
                             (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                   c-counter (rj.e/get-c-on-e system e-counter :counter)
                                   current-turn (:turn c-counter)]
                               (remove #(< (:turn %) (dec current-turn))
                                       static-buffer)))))))

(defn init-relay
  [system]
  (let [e-this (br.e/create-entity)
        e-counter (br.e/create-entity)]
    (as-> (rj.e/system<<components
            system e-this
            [[:relay {:static []
                      :blocking []}]
             [:tickable {:tick-fn process-input-tick
                         :pri -1}]
             [:renderable {:render-fn render-static-messages}]]) system
      (rj.e/system<<components
        system e-counter
        [[:counter {:turn 1}]
         [:tickable {:pri -2
                     :tick-fn (fn [_ e-this system]
                                (rj.e/upd-c system e-this :counter
                                            (fn [c-counter]
                                              (update-in c-counter [:turn]
                                                         inc))))}]]))))

