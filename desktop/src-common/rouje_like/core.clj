(ns rouje-like.core
  (:import [com.badlogic.gdx.scenes.scene2d.ui Label TextField$TextFieldListener]
           [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch])

  (:require [play-clj.core :refer :all]
            [play-clj.core :as play]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]

            [brute.entity :as br.e]
            [brute.system :as br.s]

            [rouje-like.components :as rj.c]
            [rouje-like.destructible :as rj.d]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.rendering :as rj.r]
            [rouje-like.input :as rj.in]
            [rouje-like.utils :as rj.u]
            [rouje-like.player :as rj.pl]
            [rouje-like.world :as rj.wr]
            [rouje-like.messaging :as rj.msg]))

(declare main-screen main-menu-screen rouje-like)

(def ^:private sys (atom {}))

(defn init-entities
  [system]
  (-> system
      (rj.pl/init-player)
      (rj.msg/init-relay)
      (rj.wr/init-world)

      ;; Add player
      (as-> system
            (do (println "core::add-player " (not (nil? system))) system)
            (let [e-player (first (rj.e/all-e-with-c system :player))
                  e-world (first (rj.e/all-e-with-c system :world))]
              (rj.u/update-in-world system e-world rj.pl/init-player-pos
                                    (fn [entities]
                                      (vec (conj (filter #(#{:floor} (:type %)) entities)
                                                 (rj.c/map->Entity {:id   e-player
                                                                    :type :player})))))))))

(defn register-system-fns
  [system]
  (-> system
      (br.s/add-system-fn rj.r/process-one-game-tick)))

(defscreen main-screen
  :on-show
  (fn [screen _]
    (update! screen :renderer (stage) :camera (orthographic))
    (graphics! :set-continuous-rendering false)
    (as-> (br.e/create-system) system
      (init-entities system)
      (register-system-fns system)
      (reset! sys system)))

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
                (as-> (rj.in/process-keyboard-input @sys key-code) system
                  (if (empty? (rj.e/all-e-with-c system :player))
                    (-> (br.e/create-system)
                        (init-entities)
                        (register-system-fns))
                    system))))))

  :on-fling
  (fn [screen _]
    (let [x-vel (:velocity-x screen)
          y-vel (:velocity-y screen)]
      (reset! sys
              (rj.in/process-fling-input @sys x-vel y-vel)))))

(defgame rouje-like
  :on-create
  (fn [this]
    (set-screen! this main-screen)))

