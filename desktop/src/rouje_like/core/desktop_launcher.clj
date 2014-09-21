(ns rouje-like.core.desktop-launcher
  (:require [rouje-like.core :refer :all]
            [rouje-like.components :refer [block-size]])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. rouje-like "rouje-like" (* 22 block-size) (* 23 block-size))
  (Keyboard/enableRepeatEvents false))
