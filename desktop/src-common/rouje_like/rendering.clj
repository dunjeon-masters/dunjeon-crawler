(ns rouje-like.rendering
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [com.badlogic.gdx.graphics Texture Pixmap Color]
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
           [clojure.lang Keyword Atom]
           [com.badlogic.gdx.graphics Texture Pixmap Color]
           [com.badlogic.gdx.files FileHandle])

  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer [texture texture!]]

            [clojure.math.numeric-tower :as math]

            [rouje-like.components :refer [render]]

            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.equipment :as rj.eq]
            [rouje-like.config :as rj.cfg]
            [rouje-like.entity-wrapper     :as rj.e]))

#_(in-ns 'rouje-like.rendering)
#_(use 'rouje-like.rendering :reload)

(defn process-one-game-tick
  [system delta-time]
  (let [renderable-entities (rj.e/all-e-with-c system :renderable)]
    (doseq [e-renderable renderable-entities]
      (let [c-renderable (rj.e/get-c-on-e system e-renderable :renderable)
            args (assoc (:args c-renderable)
                        :delta-time delta-time)]
        (render c-renderable e-renderable args system))))
  system)

(defn render-messages
  [_ e-this _ system]
  (let [c-relay (rj.e/get-c-on-e system e-this :relay)

        e-counter (first (rj.e/all-e-with-c system :counter))
        c-counter (rj.e/get-c-on-e system e-counter :counter)
        current-turn (:turn c-counter)

        statics (:static c-relay)
        blocking (:blocking c-relay)
        current-messages (concat
                           (filter #(= (:turn %) (dec current-turn))
                                   statics)
                           blocking)
        current-messages (mapcat #(str (:message %) ". \n")
                                 current-messages)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (apply str (into [] current-messages))
                   (color :green)
                   :set-y (float 0))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player-stats
  [_ e-this {:keys [view-port-sizes]} system]
  (let [[_ vheight] view-port-sizes

        c-player (rj.e/get-c-on-e system e-this :player)
        player-name (:name c-player)

        c-race (rj.e/get-c-on-e system e-this :race)
        race (:race c-race)

        c-class (rj.e/get-c-on-e system e-this :class)
        class (:class c-class)

        c-wallet (rj.e/get-c-on-e system e-this :wallet)
        gold (:gold c-wallet)

        c-experience (rj.e/get-c-on-e system e-this :experience)
        experience (:experience c-experience)
        level (:level c-experience)

        c-position (rj.e/get-c-on-e system e-this :position)
        x (:x c-position)
        y (:y c-position)
        z (:z c-position)

        c-destructible (rj.e/get-c-on-e system e-this :destructible)
        hp (:hp c-destructible)
        def (:def c-destructible)
        status-effects (:status-effects c-destructible)
        max-hp (:max-hp c-destructible)

        c-inv (rj.e/get-c-on-e system e-this :inventory)
        junk (count (:junk c-inv))
        slot (rj.eq/equipment-name (or (:weapon (:slot c-inv)) (:armor (:slot c-inv))))
        hp-potions (:hp-potion c-inv)
        mp-potions (:mp-potion c-inv)

        c-equip (rj.e/get-c-on-e system e-this :equipment)
        armor (rj.eq/equipment-name (:armor c-equip))
        weapon (rj.eq/equipment-name (:weapon c-equip))

        c-attacker (rj.e/get-c-on-e system e-this :attacker)
        attack (:atk c-attacker)

        c-energy (rj.e/get-c-on-e system e-this :energy)
        energy (:energy c-energy)

        c-magic (rj.e/get-c-on-e system e-this :magic)
        mp (:mp c-magic)
        max-mp (:max-mp c-magic)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Name: " player-name
                        " - Gold: [" gold "]"
                        " - Position: [" x "," y "," z "]"
                        " - HP: [" hp  "/" max-hp "]"
                        " - MP: [" mp "/" max-mp "]"
                        " - Attack: [" attack "]"
                        " - Defense: [" def "]"
                        " - Race: [" race "]"
                        " - Class: [" class "]"
                        "\nExperience: [" experience "]"
                        " - Level: [" level "]"
                        " - cli: " @rj.u/cli
                        " - Status: " status-effects
                        "\nEnergy: [" energy "]"
                        " - Junk: [" junk "]"
                        " - Slot: [" slot "]"
                        " - Armor: [" armor "]"
                        " - Weapon: [" weapon "]"
                        " - HP-Potions: [" hp-potions "]"
                        " - MP-Potions: [" mp-potions "]")

                   (color :green)
                   :set-y (float (* (+ vheight
                                       (- (+ (:top rj.cfg/padding-sizes)
                                             (:btm rj.cfg/padding-sizes))
                                          2))

                                    (rj.cfg/block-size))))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [_ e-this args system]
  (render-player-stats _ e-this args system))

(def ^:private type->tile-info
  (let [grim-tile-sheet "grim_12x12.png"
        darkond-tile-sheet "DarkondDigsDeeper_16x16.png"
        bisasam-tile-sheet "Bisasam_20x20.png"]
    {:player   {:x 0 :y 4
                :width 12 :height 12
                :color {:r 255 :g 255 :b 255 :a 255}
                :tile-sheet grim-tile-sheet}
     :wall     {:x 3 :y 2
                :width 12 :height 12
                :color {:r 255 :g 255 :b 255 :a 128}
                :tile-sheet grim-tile-sheet}
     :temple-wall {:x 3 :y 2
                   :width 12 :height 12
                   :color {:r 218 :g 165 :b 32 :a 128}
                   :tile-sheet grim-tile-sheet}
     :door     {:x 11 :y 2
                :width 12 :height 12
                :color {:r 255 :g 255 :b 255 :a 255}
                :tile-sheet grim-tile-sheet}
     :open-door {:x 12 :y 5
                 :width 12 :height 12
                 :color {:r 255 :g 255 :b 255 :a 128}
                 :tile-sheet grim-tile-sheet}
     :hidden-spike-trap {:x 14 :y 2
                         :width 12 :height 12
                         :color {:r 255 :g 140 :b 0 :a 255}
                         :tile-sheet grim-tile-sheet}
     :spike-trap {:x 4 :y 14
                  :width 16 :height 16
                  :color {:r 218 :g 165 :b 32 :a 255}
                  :tile-sheet darkond-tile-sheet}
     :arrow-trap {:x 3 :y 2
                  :width 12 :height 12
                  :color {:r 218 :g 165 :b 32 :a 255}
                  :tile-sheet grim-tile-sheet}
     :maze-wall {:x 8 :y 5
                 :width 12 :height 12
                 :color {:r 0 :g 82 :b 3 :a 255}
                 :tile-sheet grim-tile-sheet}
     :gold     {:x 1 :y 9
                :width 12 :height 12
                :color {:r 255 :g 255 :b 0 :a 255}
                :tile-sheet grim-tile-sheet}
     :lichen   {:x 15 :y 0
                :width 12 :height 12
                :color {:r 0 :g 255 :b 0 :a 255}
                :tile-sheet grim-tile-sheet}
     :floor    {:x 14 :y 2
                :width 12 :height 12
                :color {:r 255 :g 255 :b 255 :a 64}
                :tile-sheet grim-tile-sheet}
     :forest-floor    {:x 14 :y 2
                       :width 12 :height 12
                       :color {:r 103 :g 133 :b 81 :a 64}
                       :tile-sheet grim-tile-sheet}
     :merchant {:x 13 :y 4
                :width 12 :height 12
                :color {:r 0 :g 0 :b 255 :a 255}
                :tile-sheet grim-tile-sheet}
     :torch    {:x 1 :y 2
                :width 12 :height 12
                :color {:r 255 :g 0 :b 0 :a 255}
                :tile-sheet grim-tile-sheet}
     :tree     {:x 5 :y 0
                :width 20 :height 20
                :color {:r 21 :g 54 :b 21 :a 255}
                :tile-sheet bisasam-tile-sheet}
     :portal   {:x 4 :y 9
                :width 12 :height 12
                :color {:r 102 :g 0 :b 102 :a 255}
                :tile-sheet grim-tile-sheet}
     :m-portal {:x 4 :y 9
                :width 12 :height 12
                :color {:r 0 :g 0 :b 255 :a 255}
                :tile-sheet grim-tile-sheet}
     :equipment {:x 2 :y 9
                 :width 12 :height 12
                 :color {:r 255 :g 255 :b 255 :a 255}
                 :tile-sheet grim-tile-sheet}
     :purchasable {:x 2 :y 9
                   :width 12 :height 12
                   :color {:r 255 :g 255 :b 0 :a 255}
                   :tile-sheet grim-tile-sheet}
     :bat      {:x 14 :y 5
                :width 12 :height 12
                :color {:r 255 :g 255 :b 255 :a 128}
                :tile-sheet grim-tile-sheet}
     :skeleton {:x 3 :y 5
                :width 16 :height 16
                :color {:r 255 :g 255 :b 255 :a 255}
                :tile-sheet darkond-tile-sheet}
     :dune     {:x 14 :y 2
                :width 12 :height 12
                :color {:r 255 :g 140 :b 0 :a 255}
                :tile-sheet grim-tile-sheet}
     :health-potion {:x 13 :y 10
                     :width 12 :height 12
                     :color {:r 255 :g 0 :b 0 :a 255}
                     :tile-sheet grim-tile-sheet}
     :snake    {:x 3 :y 7
                :width 16 :height 16
                :color {:r 1 :g 255 :b 1 :a 255}
                :tile-sheet darkond-tile-sheet}
     :troll    {:x 4 :y 5
                :width 12 :height 12
                :color {:r 255 :g 140 :b 1 :a 255}
                :tile-sheet grim-tile-sheet}
:mimic    {:x 15 :y 8
           :width 12 :height 12
           :color {:r 255 :g 241 :b 36 :a 255}
           :tile-sheet grim-tile-sheet}
:hidden-mimic {:x 1 :y 9
               :width 12 :height 12
               :color {:r 128 :g 255 :b 1 :a 255}
               :tile-sheet grim-tile-sheet}
:spider   {:x 14 :y 9
           :width 16 :height 16
           :color {:r 183 :g 21 :b 3 :a 255}
           :tile-sheet darkond-tile-sheet}
:slime   {:x 7 :y 15
          :width 16 :height 16
          :color {:r 72 :g 223 :b 7 :a 125}
          :tile-sheet darkond-tile-sheet}
:drake   {:x 13 :y 0
          :width 16 :height 16
          :color {:r 222 :g 5 :b 48 :a 255}
          :tile-sheet darkond-tile-sheet}
:necromancer   {:x 10 :y 14
                :width 12 :height 12
                :color {:r 116 :g 84 :b 141 :a 255}
                :tile-sheet grim-tile-sheet}
:colossal_amoeba  {:x 7 :y 12
                   :width 16 :height 16
                   :color {:r 111 :g 246 :b 255 :a 125}
                   :tile-sheet darkond-tile-sheet}
:giant_amoeba  {:x 7 :y 12
                :width 16 :height 16
                :color {:r 111 :g 246 :b 255 :a 125}
                :tile-sheet darkond-tile-sheet}
:large_amoeba  {:x 7 :y 12
                :width 16 :height 16
                :color {:r 175 :g 251 :b 255 :a 125}
                :tile-sheet darkond-tile-sheet}
:willowisp  {:x 10 :y 2
             :width 20 :height 20
             :color {:r 210 :g 138 :b 181 :a 125}
             :tile-sheet bisasam-tile-sheet}
:hydra-head  {:x 6 :y 2
              :width 16 :height 16
              :color {:r 40 :g 156 :b 23 :a 255}
              :tile-sheet darkond-tile-sheet}
:hydra-neck  {:x 12 :y 1 ;;Alt: 14,7
              :width 16 :height 16
              :color {:r 40 :g 156 :b 23 :a 255}
              :tile-sheet darkond-tile-sheet}
:hydra-tail  {:x 12 :y 1
              :width 16 :height 16
              :color {:r 40 :g 156 :b 23 :a 255}
              :tile-sheet darkond-tile-sheet}
:hydra-rear  {:x 12 :y 1
              :width 16 :height 16
              :color {:r 40 :g 156 :b 23 :a 255}
              :tile-sheet darkond-tile-sheet}
:magic-potion {:x 13 :y 10
               :width 12 :height 12
               :color {:r 0 :g 0 :b 255 :a 255}
               :tile-sheet grim-tile-sheet}}))

(def ^:private type->texture
  (memoize
    (fn [^Keyword type]
      (let [tile-info (type->tile-info type)
            tile-sheet (:tile-sheet tile-info)
            width (:width tile-info)
            height (:height tile-info)
            x (* width (:x tile-info))
            y (* height (:y tile-info))
            tile-color (:color tile-info)]
        (assoc (texture tile-sheet
                        :set-region x y width height)
               :color tile-color)))))

(defn render-world
  [_ e-this {:keys [view-port-sizes]} system]
  (let [e-player (first (rj.e/all-e-with-c system :player))

        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [(:x c-player-pos)
                    (:y c-player-pos)]
        fog-of-war? (:fog-of-war? (rj.e/get-c-on-e system e-player :player))

        c-sight (rj.e/get-c-on-e system e-player :playersight)
        sight (math/ceil (:distance c-sight))

        c-world (rj.e/get-c-on-e system e-this :world)
        levels (:levels c-world)
        world (nth levels (:z c-player-pos))

        [vp-size-x vp-size-y] view-port-sizes

        start-x (max 0 (- (:x c-player-pos)
                          (int (/ vp-size-x 2))))
        start-y (max 0 (- (:y c-player-pos)
                          (int (/ vp-size-y 2))))

        end-x (+ start-x vp-size-x)
        end-x (min end-x (count world))

        end-y (+ start-y vp-size-y)
        end-y (min end-y (count (first world)))

        start-x (- end-x vp-size-x)
        start-y (- end-y vp-size-y)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (doseq [x (range start-x end-x)
            y (range start-y end-y)
            :let [tile (get-in levels [(:z c-player-pos) x y])]]
      (when (or (not fog-of-war?)
                (rj.u/can-see? world sight player-pos [x y]))
        (let [top-entity (rj.u/tile->top-entity tile)
              texture-entity (-> top-entity
                                 (:type) (type->texture))]
          (let [color-values (:color texture-entity)
                color-values (update-in color-values [:a]
                                        (fn [alpha]
                                          (if-let [c-destr (rj.e/get-c-on-e system (:id top-entity) :destructible)]
                                            (let [hp (:hp c-destr)
                                                  max-hp (:max-hp c-destr)]
                                              (max 75 (* (/ hp max-hp) alpha)))
                                           alpha)))]
            (.setColor renderer
                       (Color. (float (/ (:r color-values) 255))
                               (float (/ (:g color-values) 255))
                               (float (/ (:b color-values) 255))
                               (float (/ (:a color-values) 255)))))
          (.draw renderer
                 (:object texture-entity)
                 (float (* (+ (- x start-x)
                              (:left rj.cfg/padding-sizes))
                           (rj.cfg/block-size)))
                 (float (* (+ (- y start-y)
                              (:btm rj.cfg/padding-sizes))
                           (rj.cfg/block-size)))
                 (float (rj.cfg/block-size)) (float (rj.cfg/block-size))))))
    (.end renderer)))
