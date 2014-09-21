(ns rouje-like.rendering
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])

  (:require [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]))

(defn process-one-game-tick
  [system delta-time]
  (let [renderable-entities (reverse
                              (sort-by :pri
                                       (rj.e/all-e-with-c system :renderable)))]
    (doseq [entity renderable-entities]
      (let [c-renderable (rj.e/get-c-on-e system entity :renderable)]
        ((:render-fn c-renderable) system entity (assoc (:args c-renderable)
                                                   :delta-time delta-time)))))
  system)
