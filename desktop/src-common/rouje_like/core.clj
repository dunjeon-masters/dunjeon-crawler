(ns rouje-like.core
  (:import [com.badlogic.gdx.scenes.scene2d.ui Label TextField$TextFieldListener]
           [com.badlogic.gdx.graphics.g2d TextureRegion SpriteBatch])

  (:require [play-clj.core :refer :all]
            [play-clj.core :as play]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]

            [brute.entity :as br.e]
            [brute.system :as br.s]

            [clojure.string :as s]

            [rouje-like.destructible :as rj.d]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.rendering :as rj.r]
            [rouje-like.input :as rj.in]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.player :as rj.pl]
            [rouje-like.world :as rj.wr]
            [rouje-like.config :as rj.cfg]
            [rouje-like.messaging :as rj.msg]))

#_(in-ns 'rouje-like.core)
#_(use 'rouje-like.core :reload)
#_(on-gl (set-screen! rouje-like game-screen))

(declare game-screen main-menu-screen rouje-like)

(def ^:private sys (atom {}))
(def ^:private user (atom {}))

(defn init-entities
  [system user]
  (-> system
      (rj.pl/init-player user)
      (rj.msg/init-relay)
      (rj.wr/init-world)
      (rj.pl/add-player)))

(defn register-system-fns
  [system]
  (-> system
      (br.s/add-system-fn rj.r/process-one-game-tick)))

(defscreen game-screen
  :on-show
  (fn [screen _]
    (update! screen :renderer (stage) :camera (orthographic))
    (graphics! :set-continuous-rendering false)
    (as-> (br.e/create-system) system
      (init-entities system @user)
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
                    (init-entities @user)
                    (register-system-fns))
                (as-> (rj.in/process-keyboard-input @sys key-code) system
                  (if (empty? (rj.e/all-e-with-c system :player))
                    (-> (br.e/create-system)
                        (init-entities @user)
                        (register-system-fns))
                    system))))))

  :on-fling
  (fn [screen _]
    (let [x-vel (:velocity-x screen)
          y-vel (:velocity-y screen)]
      (reset! sys
              (rj.in/process-fling-input @sys x-vel y-vel)))))

(defn cmds->action
  [cmds]
  (let [cmd->action {"name"  (fn [n]
                               (swap! user #(assoc % :n n)))
                     "race"  (fn [r]
                               (swap! user #(assoc % :r r)))
                     "class" (fn [c]
                               (swap! user #(assoc % :c c)))
                     "start" (fn [_]
                               (? "STARTING GAME")
                               (set-screen! rouje-like game-screen)
                               (? "GAME LOADED"))}
        cmd&arg (first (partition 2 (s/split cmds #" ")))
        action (cmd->action (first cmd&arg))
        arg (second cmd&arg)]
    (if action
      (action arg)
      nil)))

(def cmdl (atom ""))
(def keycode->cmdl-action
  (let [k->cmdl-fn (fn [k] (fn [] (swap! cmdl str (name k))))
        key-codes (range 29 55) ;[a-z]
        alphabet [:a :b :c :d :e :f :g :h :i :j :k :l :m
                  :n :o :p :q :r :s :t :u :v :w :x :y :z]]
    (merge {(play/key-code :escape)    (fn []
                                         (reset! cmdl ""))
            (play/key-code :backspace) (fn []
                                         (swap! cmdl #(apply str (drop-last 1 %))))
            (play/key-code :enter)     (fn []
                                         (let [cmds @cmdl]
                                           (reset! cmdl "")
                                           (cmds->action cmds)))
            (play/key-code :space)     (fn [] (swap! cmdl str " "))}
           (zipmap key-codes (map k->cmdl-fn alphabet)))))

(defscreen main-menu-screen
  :on-show
  (fn [screen _]
    (update! screen :renderer (stage))
    (let [height (graphics! :get-height)
          width (graphics! :get-height)
          starting-x (+ -25 (* width 1/4))
          starting-y (* height 4/7)]
      (vector
        (label (str "Welcome to Dunjeon Crawler\n"
                    "type \"name <name>\" \"race <race>\" \"class <class>\"\n"
                    "and \"start game\" to begin!")
               (color :green)
               :set-x (float starting-x)
               :set-y (float (+ 50 starting-y))
               :set-width (float (* width 1/2))
               :set-wrap true)
        (assoc (label "" (color :green)
                      :set-x (float starting-x)
                      :set-y (float (+ 25 starting-y)))
               :id :user)
        (assoc (label "" (color :green)
                      :set-x (float starting-x)
                      :set-y (float (+ 0 starting-y)))
               :id :cmdl))))

  :on-render
  (fn [screen entities]
    (clear!)
    (->> (for [entity entities]
           (case (:id entity)
             :cmdl (doto entity
                     (label! :set-text (str ">? " @cmdl)))
             :user (doto entity
                     (label! :set-text (str @user)))
             entity))
         (render! screen)))

  :on-key-down
  (fn [screen entities]
    (let [key-code (:key screen)
          cmdl-action (keycode->cmdl-action key-code)]
      (when cmdl-action
        (cmdl-action)))
    entities))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                            (catch Exception e
                              (.printStackTrace e)
                              (reset! sys {})
                              (set-screen! rouje-like screen)))))

(defgame rouje-like
  :on-create
  (fn [this]
    (set-screen! this main-menu-screen)))
