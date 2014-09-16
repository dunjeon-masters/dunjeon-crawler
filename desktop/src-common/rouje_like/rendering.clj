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
  (let [entities (rj.e/all-e system :renderable)]
        (doseq [entity entities]
          (let [component (rj.e/get-c system entity :renderable)]
            ((:fn component) system entity))))
  system)
