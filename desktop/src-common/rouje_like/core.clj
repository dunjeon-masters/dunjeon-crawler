(ns rouje-like.core
  (:import [com.badlogic.gdx.scenes.scene2d.ui Label]
           [com.badlogic.gdx.graphics.g2d TextureRegion])

  (:require [play-clj.core :refer :all]
            [play-clj.core :as play]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]

            [brute.entity :as br.e]
            [brute.system :as br.s]

            [rouje-like.components :as rj.c]
            [rouje-like.destructible :as rj.d]
            [rouje-like.entity :as rj.e]
            [rouje-like.rendering :as rj.r]
            [rouje-like.bat :as rj.bt]
            [rouje-like.input :as rj.in]
            [rouje-like.lichen :as rj.lc]
            [rouje-like.player :as rj.pl]
            [rouje-like.world :as rj.wr]
            [rouje-like.attacker :as rj.atk]))

(declare main-screen rouje-like)

(def ^:private sys (atom 0))

(def ^:private world-sizes [60 60])
(def ^:private init-wall% 45)
(def ^:private init-torch% 2)
(def ^:private init-gold% 5)
(def ^:private init-lichen% 1)
(def ^:private init-player-x-pos (/ (world-sizes 0) 2))
(def ^:private init-player-y-pos (/ (world-sizes 1) 2))
(def ^:private init-player-pos [init-player-x-pos init-player-y-pos])
(def ^:private init-player-moves 250)
(def ^:private init-sight-distance 5.0)
(def ^:private init-sight-decline-rate (/ 1 5))
(def ^:private init-sight-lower-bound 5)                    ;; Inclusive
(def ^:private init-sight-upper-bound 13)                   ;; Exclusive
(def ^:private init-sight-torch-power 2)

(defn init-entities
  [system]
  (let [e-world  (br.e/create-entity)
        e-player (br.e/create-entity)]
    (-> system
        (rj.e/add-e e-player)
        (rj.e/add-c e-player (rj.c/map->Player {:show-world? false}))
        (rj.e/add-c e-player (rj.c/map->Position {:x init-player-x-pos
                                                  :y init-player-y-pos}))
        (rj.e/add-c e-player (rj.c/map->Mobile {:can-move? rj.pl/can-move?
                                                :move      rj.pl/move}))
        (rj.e/add-c e-player (rj.c/map->Digger {:can-dig? rj.pl/can-dig?
                                                :dig      rj.pl/dig}))
        (rj.e/add-c e-player (rj.c/map->Attacker {:atk         1
                                                  :can-attack?-fn rj.atk/can-attack?
                                                  :attack-fn      rj.atk/attack}))
        (rj.e/add-c e-player (rj.c/map->MovesLeft {:moves-left init-player-moves}))
        (rj.e/add-c e-player (rj.c/map->Gold {:gold 0}))
        (rj.e/add-c e-player (rj.c/map->Sight {:distance     (inc init-sight-distance)
                                               :decline-rate init-sight-decline-rate
                                               :lower-bound  init-sight-lower-bound
                                               :upper-bound  init-sight-upper-bound
                                               :torch-power  init-sight-torch-power}))
        (rj.e/add-c e-player (rj.c/map->Renderable {:pri       0
                                                    :render-fn rj.pl/render-player
                                                    :args      {:view-port-sizes rj.c/view-port-sizes}}))
        (rj.e/add-c e-player (rj.c/map->Destructible {:hp      25
                                                      :defense 1
                                                      :take-damage-fn rj.d/take-damage}))

        (rj.e/add-e e-world)
        (rj.e/add-c e-world (rj.c/map->World {:world (rj.wr/generate-random-world
                                                       world-sizes init-wall%
                                                       init-torch% init-gold%)}))
        (rj.e/add-c e-world (rj.c/map->Renderable {:pri       1
                                                   :render-fn rj.wr/render-world
                                                   :args      {:view-port-sizes rj.c/view-port-sizes}}))

        ;; Spawn lichens
        (as-> system
              (nth (iterate rj.lc/add-lichen system)
                   (* (/ init-lichen% 100)
                      (apply * world-sizes))))

        ;; Spawn bats
        (as-> system
              (nth (iterate rj.bt/add-bat system)
                   20))

        ;; Add player
        (rj.wr/update-in-world e-world init-player-pos
                               (fn [entities _]
                                 (vec (conj entities
                                            (rj.c/map->Entity {:id   e-player
                                                               :type :player}))))))))

(defn register-system-fns
  [system]
  (-> system
      (br.s/add-system-fn  rj.r/process-one-game-tick)))

(defscreen main-screen
  :on-show
  (fn [screen _]
    (update! screen :renderer (stage) :camera (orthographic))
    (graphics! :set-continuous-rendering false)
    (-> (br.e/create-system)
        (init-entities)
        (register-system-fns)
        (as-> system
              (reset! sys system))))
  
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
      (reset! sys
              (if (= key-code (play/key-code :f5))
                (-> (br.e/create-system)
                    (init-entities)
                    (register-system-fns))
                (-> (rj.in/process-keyboard-input! @sys key-code)
                    (as-> system
                          (if (empty? (rj.e/all-e-with-c system :player))
                            (-> (br.e/create-system)
                                (init-entities)
                                (register-system-fns))
                            system)))))))

  :on-fling
  (fn [screen _]
    (let [x-vel (:velocity-x screen)
          y-vel (:velocity-y screen)]
      (reset! sys
              (rj.in/process-fling-input! @sys x-vel y-vel)))))

(defgame rouje-like
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
