(ns rouje-like.core.desktop-launcher
  (:require [rouje-like.core :refer [rouje-like]]
            [rouje-like.components :refer [block-size padding-sizes view-port-sizes]])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. rouje-like "rouje-like"
                     (* (+ (* 2 (:x padding-sizes)) (view-port-sizes 0)) block-size)
                     (* (+ (* 2 (:y padding-sizes)) (view-port-sizes 0)) block-size))
  (Keyboard/enableRepeatEvents true))
