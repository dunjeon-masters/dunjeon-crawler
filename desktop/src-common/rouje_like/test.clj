(ns rouje-like.test
  (:require [lanterna.screen :as s]))

(def screen (s/get-screen :swing))
(s/start screen)

(s/put-string screen 0 0 "Hello, World!")
(s/put-string screen 10 10 "Anthony!")
(s/put-string screen 0 1 "RED" {:fg :red
                                :bg :green})
(s/put-string screen 0 0 " ")
(s/move-cursor screen 7 0)

(s/redraw screen)

(s/put-string screen 5 5 (str (s/get-key screen)))
(s/stop screen)


