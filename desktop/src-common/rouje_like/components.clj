(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(defrecord Bat [])

(defrecord Broadcaster [name-fn])

(defrecord Counter [turn])

(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord Energy [energy])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Experience [experience
                       level
                       level-up-fn])

(defrecord Gold [value])

(defrecord Item [pickup-fn])

(defrecord Killable [experience])

(defrecord Klass [class])

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord Player [name
                   show-world?])

(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-power])

(defrecord Portal [^Number x ^Number y ^Number z])

(defrecord Position [x y z
                     ^Keyword type])

(defrecord Race [race])

(defrecord Receiver [])

(defrecord Relay [static
                  blocking])

(defrecord Sight [distance])

(defrecord Skeleton [])

(defrecord Tile [^Number x ^Number y ^Number z
                 ^PersistentVector entities])

(defrecord Torch [brightness])

(defrecord Trap [])

(defrecord Wallet [^Number gold])

(defrecord World [levels
                  add-level-fn])

(defprotocol IAttacker
  (can-attack? [this e-this e-target system])
  (attack      [this e-this e-target system]))
(defrecord Attacker [^Number atk
                     status-effects
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
                         ^Number max-hp
                         ^Number def
                         status-effects
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
(defrecord Tickable [^Fn tick-fn pri]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))

(def ^{:doc "Workaround for not being able to get record's type 'statically'"}
  get-type {:attacker     (type (->Attacker nil nil nil nil nil))
            :bat          (type (->Bat))
            :broadcaster  (type (->Broadcaster nil))
            :class        (type (->Klass nil))
            :counter      (type (->Counter nil))
            :destructible (type (->Destructible nil nil nil nil nil nil))
            :digger       (type (->Digger nil nil))
            :energy       (type (->Energy nil))
            :entity       (type (->Entity nil nil))
            :experience   (type (->Experience nil nil nil))
            :gold         (type (->Gold nil))
            :item         (type (->Item nil))
            :killable     (type (->Killable nil))
            :lichen       (type (->Lichen nil nil))
            :mobile       (type (->Mobile nil nil))
            :player       (type (->Player nil nil))
            :playersight  (type (->PlayerSight nil nil nil nil nil))
            :portal       (type (->Portal nil nil nil))
            :position     (type (->Position nil nil nil nil))
            :race         (type (->Race nil))
            :receiver     (type (->Receiver))
            :relay        (type (->Relay nil nil))
            :renderable   (type (->Renderable nil nil))
            :sight        (type (->Sight nil))
            :skeleton     (type (->Skeleton))
            :tickable     (type (->Tickable nil nil))
            :tile         (type (->Tile nil nil nil nil))
            :torch        (type (->Torch nil))
            :trap         (type (->Trap))
            :wallet       (type (->Wallet nil))
            :world        (type (->World nil nil))})
