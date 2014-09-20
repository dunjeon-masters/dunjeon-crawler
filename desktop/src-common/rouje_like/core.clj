(ns rouje-like.core
  (:import [com.badlogic.gdx.scenes.scene2d.ui Label]
           [com.badlogic.gdx.graphics.g2d TextureRegion])

  (:require [play-clj.core :refer :all]
            [play-clj.ui   :refer :all]
            [play-clj.g2d  :refer :all]

            [brute.entity :as br.e]
            [brute.system :as br.s]

            [rouje-like.components :as rj.c]
            [rouje-like.entity     :as rj.e]
            [rouje-like.rendering  :as rj.r]
            [rouje-like.input      :as rj.in]
            [rouje-like.player     :as rj.pl]
            [rouje-like.world      :as rj.wr]))

(declare main-screen rouje-like)

(def ^:private sys (atom 0))

(def ^:private world-sizes [30 30])
(def ^:private init-wall% 40)
(def ^:private init-torch% 2)
(def ^:private init-gold% 5)
(def ^:private init-player-x-pos (atom (/ (world-sizes 0) 2)))
(def ^:private init-player-y-pos (atom (/ (world-sizes 1) 2)))
(def ^:private init-player-moves 150)
(def ^:private init-sight-distance 5.0)
(def ^:private init-sight-decline-rate (/ 1 3))
(def ^:private init-sight-lower-bound 4)                    ;; Inclusive
(def ^:private init-sight-upper-bound 9)                    ;; Exclusive

(defn ^:private start
  [system]
  (let [e-world  (br.e/create-entity)
        e-player (br.e/create-entity)
        world (atom (rj.wr/generate-random-world
                      world-sizes init-wall%
                      init-torch% init-gold%
                      [init-player-x-pos init-player-y-pos]))]
    (-> system
        (rj.e/add-e e-player)
        (rj.e/add-c e-player (rj.c/->Player world (atom false)))
        (rj.e/add-c e-player (rj.c/->Position init-player-x-pos init-player-y-pos
                                              rj.pl/can-move? rj.pl/move!))
        (rj.e/add-c e-player (rj.c/->Digger rj.pl/can-dig? rj.pl/dig!))
        (rj.e/add-c e-player (rj.c/->MovesLeft (atom init-player-moves)))
        (rj.e/add-c e-player (rj.c/->Gold (atom 0)))
        (rj.e/add-c e-player (rj.c/map->Sight {:distance (atom (inc init-sight-distance))
                                               :decline-rate (atom init-sight-decline-rate)
                                               :lower-bound (atom init-sight-lower-bound)
                                               :upper-bound (atom init-sight-upper-bound)}))
        (rj.e/add-c e-player (rj.c/map->Renderable {:pri 0
                                                    :render-fn rj.pl/render-player
                                                    :args {:world-sizes world-sizes}}))
        (rj.e/add-e e-world)
        (rj.e/add-c e-world (rj.c/->World world))
        (rj.e/add-c e-world (rj.c/map->Renderable {:pri 1
                                                   :render-fn rj.wr/render-world
                                                   :args nil})))))

(defn ^:private register-system-fns
  [system]
  (-> system
      (rj.r/start)
      (br.s/add-system-fn  rj.r/process-one-game-tick)))

(defscreen main-screen
  :on-show
  (fn [screen _]
    (update! screen :renderer (stage) :camera (orthographic))
    (-> (br.e/create-system)
        (start)
        (register-system-fns)
        (as-> s (reset! sys s))))                           ;; TODO: WHAT IS THIS EVEN?
  
  :on-render
  (fn [screen _]
    (clear!)
    (render! screen)
    (reset! sys
            (br.s/process-one-game-tick @sys
                                        (graphics! :get-delta-time))))

  :on-key-down
  (fn [screen _]
    (let [key-code (:key screen)]
      (rj.in/process-keyboard-input @sys key-code)))

  :on-fling
  (fn [screen _]
    (let [x-vel (:velocity-x screen)
          y-vel (:velocity-y screen)]
      (rj.in/process-fling-input @sys x-vel y-vel))))

(defgame rouje-like
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
