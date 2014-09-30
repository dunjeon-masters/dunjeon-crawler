(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(def block-size 48)
(def view-port-sizes [20 20])
(def padding-sizes {:top   1
                    :btm   1
                    :left  1
                    :right 1})
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Player [show-world?])

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord Bat [])

(defrecord Skeleton [])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord World [world])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord Entity [^Keyword id
                   ^Keyword type])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord MovesLeft [moves-left])

(defrecord Gold [gold])

(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-power])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Position [x y
                     ^Keyword type])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defrecord Sight [distance])
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IDestructible
  (take-damage [this e-this damage from system]))
(defrecord Destructible [hp
                         defense
                         can-retaliate?
                         ^Fn take-damage-fn]
  IDestructible
  (take-damage     [this e-this damage from system]
    (take-damage-fn this e-this damage from system)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol ITickable
  (tick [this e-this system]))
(defrecord Tickable [^Fn tick-fn]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol IRenderable
  (render [this e-this args system]))
(defrecord Renderable [^Fn render-fn
                       args]
  IRenderable
  (render     [this e-this argz system]
    (render-fn this e-this argz system)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Workaround for not being able to get record's type "statically"
(def get-type {:world        (type (->World nil))
               :tile         (type (->Tile nil nil nil))
               :entity       (type (->Entity nil nil))
               :player       (type (->Player nil))
               :digger       (type (->Digger nil nil))
               :position     (type (->Position nil nil nil))
               :mobile       (type (->Mobile nil nil))
               :moves-left   (type (->MovesLeft nil))
               :gold         (type (->Gold nil))
               :playersight  (type (->PlayerSight nil nil nil nil nil))
               :sight        (type (->Sight nil))
               :renderable   (type (->Renderable nil nil))
               :attacker     (type (->Attacker nil nil nil nil))
               :destructible (type (->Destructible nil nil nil nil))
               :tickable     (type (->Tickable nil))
               :lichen       (type (->Lichen nil nil))
               :bat          (type (->Bat))
               :skeleton     (type (->Skeleton))})
