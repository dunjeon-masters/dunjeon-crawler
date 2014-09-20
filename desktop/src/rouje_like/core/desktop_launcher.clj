(ns rouje-like.core.desktop-launcher
  (:require [rouje-like.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. rouje-like "rouje-like" 810 860)
  (Keyboard/enableRepeatEvents false))
