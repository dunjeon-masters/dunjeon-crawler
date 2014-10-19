(ns rouje-like.relay
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]))

(defn render-static-messages
  [_ e-this _ system]
  (let [_ (println "render-static-messages")
        c-relay (rj.e/get-c-on-e system e-this :relay)
        static-messages (last (:static c-relay))
        _ (println static-messages)
        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str static-messages)
                   (color :green)
                   :set-y (float (* 1 rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn process-input-tick
  [_ e-this system]
  (render-static-messages nil e-this nil system)
  (-> system
      (identity)
      #_(rj.e/upd-c e-this :relay
                  (fn [c-relay]
                    (assoc c-relay :static [])))))

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