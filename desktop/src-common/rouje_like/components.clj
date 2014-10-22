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

(defrecord Player [show-world?])

(defrecord Lichen [^Number grow-chance%
                   ^Number max-blob-size])

(defrecord Bat [])

(defrecord Skeleton [])

(defrecord Gold [value])

(defrecord Torch [brightness])

(defrecord Class- [class])

(defrecord Race [race])

(defrecord Relay [static
                  blocking])

(defrecord Counter [turn])

(defrecord Receiver [])

(defrecord Broadcaster [msg-fn])

(defrecord World [world])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord MovesLeft [^Number moves-left])

(defrecord Wallet [^Number gold])

(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-multiplier])

(defrecord Position [^Number x ^Number y
                     ^Keyword type])

(defrecord Sight [^Number distance])

(defrecord Item [pickup-fn])

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

(defprotocol IAttacker
  (can-attack? [this e-this e-target system])
  (attack      [this e-this e-target system]))
(defrecord Attacker [^Number atk
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
(defrecord Destructible [^Number hp
                         ^Number defense
                         can-retaliate?
                         ^Fn take-damage-fn]
  IDestructible
  (take-damage     [this e-this damage from system]
    (take-damage-fn this e-this damage from system)))

(defprotocol ITickable
  (tick [this e-this system]))
(defrecord Tickable [^Fn tick-fn
                     ^Number pri]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))

(defprotocol IRenderable
  (render [this e-this args system]))
(defrecord Renderable [^Fn render-fn
                       args]
  IRenderable
  (render     [this e-this argz system]
    (render-fn this e-this argz system)))

;; Workaround for not being able to get record's type "statically"
(def get-type {:attacker     (type (->Attacker nil nil nil nil))
               :bat          (type (->Bat))
               :broadcaster  (type (->Broadcaster nil))
               :counter      (type (->Counter nil))
               :destructible (type (->Destructible nil nil nil nil))
               :digger       (type (->Digger nil nil))
               :entity       (type (->Entity nil nil))
               :gold         (type (->Gold nil))
               :item         (type (->Item nil))
               :lichen       (type (->Lichen nil nil))
               :mobile       (type (->Mobile nil nil))
               :moves-left   (type (->MovesLeft nil))
               :player       (type (->Player nil))
               :playersight  (type (->PlayerSight nil nil nil nil nil))
               :position     (type (->Position nil nil nil))
               :receiver     (type (->Receiver))
               :relay        (type (->Relay nil nil))
               :renderable   (type (->Renderable nil nil))
               :sight        (type (->Sight nil))
               :skeleton     (type (->Skeleton))
               :tickable     (type (->Tickable nil nil))
               :tile         (type (->Tile nil nil nil))
               :torch        (type (->Torch nil))
               :wallet       (type (->Wallet nil))
               :world        (type (->World nil))})
