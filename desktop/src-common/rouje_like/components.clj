(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(defrecord Bat [])

(defrecord Broadcaster [name-fn])

(defrecord Counter [turn])

(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord Drake [])

(defrecord Energy [energy])


(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Equipment [equipment])

(defrecord Experience [experience
                       level
                       level-up-fn])

(defrecord Giant_amoeba [])

(defrecord Gold [value])

(defrecord HydraHead [])

(defrecord HydraNeck [])

(defrecord HydraTail [])

(defrecord Inventory [slot junk])

(defrecord Item [pickup-fn])

(defrecord Killable [experience])

(defrecord Klass [class])

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord Large_amoeba [])

(defrecord Mimic [])

(defrecord Necromancer [])

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

(defrecord Slime [])

(defrecord Snake [])

(defrecord Spider [])

(defrecord Troll [])

(defrecord Tile [^Number x ^Number y ^Number z
                 ^PersistentVector entities])

(defrecord Torch [brightness])

(defrecord Wallet [^Number gold])

(defrecord Willowisp [])

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
                         ^Fn take-damage-fn
                         on-death-fn]
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
            :destructible (type (->Destructible nil nil nil nil nil nil nil))
            :digger       (type (->Digger nil nil))
            :drake        (type (->Drake))
            :energy       (type (->Energy nil))
            :entity       (type (->Entity nil nil))
            :equipment    (type (->Equipment nil))
            :experience   (type (->Experience nil nil nil))
            :gold         (type (->Gold nil))
            :giant_amoeba (type (->Giant_amoeba))
            :hydra-head   (type (->HydraHead))
            :hydra-neck   (type (->HydraNeck))
            :hydra-tail   (type (->HydraTail))
            :inventory    (type (->Inventory nil nil))
            :item         (type (->Item nil))
            :killable     (type (->Killable nil))
            :large_amoeba (type (->Large_amoeba))
            :lichen       (type (->Lichen nil nil))
            :mobile       (type (->Mobile nil nil))
            :mimic        (type (->Mimic))
            :necromancer  (type (->Necromancer))
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
            :slime        (type (->Slime))
            :spider       (type (->Spider))
            :snake        (type (->Snake))
            :tickable     (type (->Tickable nil nil))
            :tile         (type (->Tile nil nil nil nil))
            :torch        (type (->Torch nil))
            :troll        (type (->Troll))
            :wallet       (type (->Wallet nil))
            :willowisp    (type (->Willowisp))
            :world        (type (->World nil nil))})
