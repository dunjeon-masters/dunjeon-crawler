(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(def block-size 48)
(def view-port-sizes [20 20])

;; TODO: Change to :top :btm :left :right
(def padding-sizes {:x 1 :y 1})



(defrecord World [world])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Player [show-world?])

(defrecord Digger [^Fn can-dig?
                   ^Fn dig])

(defrecord MovesLeft [moves-left])

(defrecord Gold [gold])

(defrecord Sight [distance
                  decline-rate
                  lower-bound
                  upper-bound
                  torch-power])

(defrecord Position [x y])

(defrecord Mobile [^Fn can-move?
                   ^Fn move])

(defrecord Attacker [atk
                     ^Fn attack
                     ^Fn can-attack?])

(defprotocol IDestructible
  (take-damage [this e-this damage from system]))
(defrecord Destructible [hp
                         defense
                         ^Fn take-damage-fn]
  IDestructible
  (take-damage [this e-this damage from system]
    (take-damage-fn this e-this damage from system)))

(defprotocol ITickable
  (tick [this e-this system]))
(defrecord Tickable [^Fn tick-fn]
  ITickable
  (tick [this e-this system]
    (tick-fn this e-this system)))

(defprotocol IRenderable
  (render [this e-this args system]))
(defrecord Renderable [^Fn render-fn
                       args]
  IRenderable
  (render [this e-this argz system]
    (render-fn this e-this argz system)))

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord Bat [])

;; Workaround for not being able to get record's type "statically"
(def get-type {:world        (type (->World nil))
               :tile         (type (->Tile nil nil nil))
               :entity       (type (->Entity nil nil))
               :player       (type (->Player nil))
               :digger       (type (->Digger nil nil))
               :position     (type (->Position nil nil))
               :mobile       (type (->Mobile nil nil))
               :moves-left   (type (->MovesLeft nil))
               :gold         (type (->Gold nil))
               :sight        (type (->Sight nil nil nil nil nil))
               :renderable   (type (->Renderable nil nil))
               :attacker     (type (->Attacker nil nil nil))
               :destructible (type (->Destructible nil nil nil))
               :tickable     (type (->Tickable nil))
               :lichen       (type (->Lichen nil nil))
               :bat          (type (->Bat))})
