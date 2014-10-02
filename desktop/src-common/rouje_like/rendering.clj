(ns rouje-like.rendering
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])

  (:require [rouje-like.components :refer [render]]

            [rouje-like.entity-wrapper     :as rj.e]))

(defn process-one-game-tick
  [system delta-time]
  (let [renderable-entities (rj.e/all-e-with-c system :renderable)]
    (doseq [e-renderable renderable-entities]
      (let [c-renderable (rj.e/get-c-on-e system e-renderable :renderable)
            args (assoc (:args c-renderable)
                   :delta-time delta-time)]
        (render c-renderable e-renderable args system))))
  system)
