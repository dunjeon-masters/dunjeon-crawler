(ns rouje-like.core.desktop-launcher
  (:require [rouje-like.core :refer :all]
            [rouje-like.components :refer [block-size]])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. rouje-like "rouje-like" (* (+ 2 (view-port-sizes 0)) block-size) (* (+ 3 (view-port-sizes 0)) block-size))
  (Keyboard/enableRepeatEvents false))
