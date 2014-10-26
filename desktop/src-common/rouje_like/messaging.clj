(ns rouje-like.messaging
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]))

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
  (-> system
      (rj.e/upd-c e-this :relay
                  (fn [c-relay]
                    (update-in c-relay [:static]
                               (fn [static-buffer]
                                 (let [e-counter (first (rj.e/all-e-with-c system :counter))
                                       c-counter (rj.e/get-c-on-e system e-counter :counter)
                                       current-turn (:turn c-counter)]
                                   (remove #(< (:turn %) (dec current-turn))
                                           static-buffer))))))))

(defn init-relay
  [system]
  (let [e-this (br.e/create-entity)]
    (-> system
        (rj.e/add-e e-this)
        (rj.e/add-c e-this (rj.c/map->Relay {:static []
                                             :blocking []}))
        (rj.e/add-c e-this (rj.c/map->Tickable {:tick-fn process-input-tick
                                                :pri -1}))
        (rj.e/add-c e-this (rj.c/map->Renderable {:render-fn render-static-messages})))))

