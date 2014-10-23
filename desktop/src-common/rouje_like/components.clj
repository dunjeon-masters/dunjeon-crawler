(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(def block-size 36)
(def world-sizes {:width  60
                  :height 60})
(def view-port-sizes [20 20])
(def padding-sizes {:top   1
                    :btm   1
                    :left  1
                    :right 1})

(defrecord Bat [])

(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord Class- [class])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Experience [experiece])

(defrecord Gold [gold])

(defrecord Item [pickup-fn])

(defrecord Killable [experience])

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord MovesLeft [moves-left])

(defrecord Player [show-world?])

(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-power])

(defrecord Position [x y
                     ^Keyword type])

(defrecord Race [race])

(defrecord Sight [distance])

(defrecord Skeleton [])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord World [world])

(defprotocol IAttacker
  (can-attack? [this e-this e-target system])
  (attack      [this e-this e-target system]))
(defrecord Attacker [atk
                     ^Fn attack-fn
                     ^Fn can-attack?-fn
                     is-valid-target?]
  IAttacker
  (can-attack?     [this e-this e-target system]
    (can-attack?-fn this e-this e-target system))
  (attack     [this e-this e-target system]
    (attack-fn this e-this e-target system)))

(defprotocol IDestructible
  (take-damage [this e-this damage from system]))
(defrecord Destructible [hp
                         defense
                         can-retaliate?
                         ^Fn take-damage-fn]
  IDestructible
  (take-damage     [this e-this damage from system]
    (take-damage-fn this e-this damage from system)))

(defprotocol IMobile
  (can-move? [this e-this target-tile system])
  (move      [this e-this target-tile system]))
(defrecord Mobile [^Fn can-move?-fn
                   ^Fn move-fn]
  IMobile
  (can-move?     [this e-this target-tile system]
    (can-move?-fn this e-this target-tile system))
  (move     [this e-this target-tile system]
    (move-fn this e-this target-tile system)))

(defprotocol IRenderable
  (render [this e-this args system]))
(defrecord Renderable [^Fn render-fn
                       args]
  IRenderable
  (render     [this e-this argz system]
    (render-fn this e-this argz system)))

(defprotocol ITickable
  (tick [this e-this system]))
(defrecord Tickable [^Fn tick-fn]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))

;; Workaround for not being able to get record's type "statically"
(def get-type {:attacker     (type (->Attacker nil nil nil nil))
               :bat          (type (->Bat))
               :class        (type (->Class- nil))
               :destructible (type (->Destructible nil nil nil nil))
               :digger       (type (->Digger nil nil))
               :entity       (type (->Entity nil nil))
               :experience   (type (->Experience nil))
               :gold         (type (->Gold nil))
               :item         (type (->Item nil))
               :killable     (type (->Killable nil))
               :lichen       (type (->Lichen nil nil))
               :mobile       (type (->Mobile nil nil))
               :moves-left   (type (->MovesLeft nil))
               :player       (type (->Player nil))
               :playersight  (type (->PlayerSight nil nil nil nil nil))
               :position     (type (->Position nil nil nil))
               :race         (type (->Race nil))
               :renderable   (type (->Renderable nil nil))
               :sight        (type (->Sight nil))
               :skeleton     (type (->Skeleton))
               :tickable     (type (->Tickable nil))
               :tile         (type (->Tile nil nil nil))
               :world        (type (->World nil))})
