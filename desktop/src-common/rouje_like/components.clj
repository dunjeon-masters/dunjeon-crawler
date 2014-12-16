(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

#_(use 'rouje-like.components :reload)

(defprotocol ISaveState
  (->save-state [this]))
(defprotocol IPoint
  (->3DPoint [this])
  (->2DPoint [this]))

(defrecord ArrowTrap [dir
                      ready?])

(defrecord Bat [])

(defrecord Broadcaster [name-fn])

(defrecord ColossalAmoeba [])

(defrecord Counter [turn])

(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord Energy [energy]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord Drake [])

(defrecord Door [])


(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Equipment [weapon
                      armor]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord Experience [experience
                       level
                       level-up-fn]
  ISaveState
  (->save-state [this]
    (let [this (dissoc this :level-up-fn)]
      (zipmap (keys this) (vals this)))))

(defrecord Fireball [])

(defrecord GiantAmoeba [])

(defrecord Gold [value])

(defrecord HydraHead [])

(defrecord HydraNeck [])

(defrecord HydraTail [])

(defrecord HydraRear [])

(defrecord Inventory [slot junk])

(defrecord Inspectable [msg])

(defrecord Inventory [slot
                      junk
                      hp-potion
                      mp-potion]
  ISaveState
  (->save-state [this]
    (let [saved-slot (:slot this)
          saved-slot (if saved-slot
                       (->save-state saved-slot)
                       nil)]
      {:junk (map #(->save-state %)
                  (:junk this))
       :hp-potion (:hp-potion this)
       :slot saved-slot})))

(defrecord Item [pickup-fn])

(defrecord Killable [experience])

(defrecord Klass [class]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord Magic [mp
                  max-mp
                  spells])

(defrecord LargeAmoeba [])

(defrecord Mimic [])

(defrecord Necromancer [])

(defrecord MPortal [^Number x ^Number y ^Number z])

(defrecord Merchant [])

(defrecord Player [name
                   show-world?]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-power]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord Portal [^Number x ^Number y ^Number z])

(defrecord Position [x y z
                     ^Keyword type]
  IPoint
  (->3DPoint [this]
    [z x y])
  (->2DPoint [this]
    [x y]))

(defrecord Purchasable [value])

(defrecord Race [race]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord Receiver [])

(defrecord Relay [static
                  blocking])

(defrecord Sight [distance])

(defrecord Skeleton [])

(defrecord Slime [])

(defrecord Snake [])

(defrecord Spider [])

(defrecord Troll [])

(defrecord SpikeTrap [visible?])

(defrecord Tile [^Number x ^Number y ^Number z
                 ^PersistentVector entities])

(defrecord Torch [brightness])

(defrecord Trap [])

(defrecord Wallet [^Number gold]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(defrecord WillowWisp [])

(defrecord World [levels
                  add-level-fn])

(defprotocol IAttacker
  (can-attack? [this e-this e-target system])
  (attack      [this e-this e-target system]))
(defrecord Attacker [^Number atk
                     status-effects
                     ^Fn attack-fn
                     ^Fn can-attack?-fn
                     ^Fn is-valid-target?]
  ISaveState
  (->save-state [this]
    {:atk (:atk this)})
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
  ISaveState
  (->save-state [this]
    (let [this (dissoc this :take-damage-fn)]
      (zipmap (keys this) (vals this))))
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

(def get-type
  "Workaround for not being able to get record's type 'statically'"
  {:arrow-trap       (type (->ArrowTrap nil nil))
   :attacker         (type (->Attacker nil nil nil nil nil))
   :bat              (type (->Bat))
   :broadcaster      (type (->Broadcaster nil))
   :class            (type (->Klass nil))
   :colossal-amoeba  (type (->ColossalAmoeba))
   :counter          (type (->Counter nil))
   :destructible     (type (->Destructible nil nil nil nil nil nil nil))
   :digger           (type (->Digger nil nil))
   :door             (type (->Door))
   :drake            (type (->Drake))
   :energy           (type (->Energy nil))
   :entity           (type (->Entity nil nil))
   :equipment        (type (->Equipment nil nil))
   :experience       (type (->Experience nil nil nil))
   :fireball         (type (->Fireball))
   :gold             (type (->Gold nil))
   :giant-amoeba     (type (->GiantAmoeba))
   :hydra-head       (type (->HydraHead))
   :hydra-neck       (type (->HydraNeck))
   :hydra-tail       (type (->HydraTail))
   :hydra-rear       (type (->HydraRear))
   :inventory        (type (->Inventory nil nil nil nil))
   :inspectable      (type (->Inspectable nil))
   :item             (type (->Item nil))
   :killable         (type (->Killable nil))
   :large-amoeba     (type (->LargeAmoeba))
   :lichen           (type (->Lichen nil nil))
   :magic            (type (->Magic nil nil nil))
   :m-portal         (type (->MPortal nil nil nil))
   :merchant         (type (->Merchant))
   :mobile           (type (->Mobile nil nil))
   :mimic            (type (->Mimic))
   :necromancer      (type (->Necromancer))
   :player           (type (->Player nil nil))
   :playersight      (type (->PlayerSight nil nil nil nil nil))
   :portal           (type (->Portal nil nil nil))
   :position         (type (->Position nil nil nil nil))
   :purchasable      (type (->Purchasable nil))
   :race             (type (->Race nil))
   :receiver         (type (->Receiver))
   :relay            (type (->Relay nil nil))
   :renderable       (type (->Renderable nil nil))
   :sight            (type (->Sight nil))
   :skeleton         (type (->Skeleton))
   :slime            (type (->Slime))
   :spider           (type (->Spider))
   :spike-trap       (type (->SpikeTrap nil))
   :snake            (type (->Snake))
   :tickable         (type (->Tickable nil nil))
   :tile             (type (->Tile nil nil nil nil))
   :torch            (type (->Torch nil))
   :trap             (type (->Trap))
   :troll            (type (->Troll))
   :wallet           (type (->Wallet nil))
   :willow-wisp      (type (->WillowWisp))
   :world            (type (->World nil nil))})
