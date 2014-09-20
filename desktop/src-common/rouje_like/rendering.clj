(ns rouje-like.rendering
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])

  (:require [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]))

(defn start
  [system]
  system)

(defn process-one-game-tick
  [system _]
  (let [renderable-entities (sort-by :pri
                                     (rj.e/all-e-with-c system :renderable))]
    (doseq [entity renderable-entities]
      (let [component (rj.e/get-c-on-e system entity :renderable)]
        ((:fn component) system entity (:args component)))))
  system)
