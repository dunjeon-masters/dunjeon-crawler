(ns clj-rouje-like.core.desktop-launcher
  (:require [clj-rouje-like.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. clj-rouje-like "clj-rouje-like" 800 600)
  (Keyboard/enableRepeatEvents true))
