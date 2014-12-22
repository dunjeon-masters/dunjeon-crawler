(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Keyword PersistentVector]
           [java.util UUID])
  (:require [clojure.core.typed :as t
             :refer [ann-record ann-protocol
                     U Any Map Set Kw Fn IFn Num Bool Seqable]]))

#_(use 'rouje-like.components :reload)

(t/defprotocol ISaveState
  (->save-state [this] :- (Map Kw Any)))
(t/defprotocol IPoint
  (->3DPoint [this] :- (Seqable Num))
  (->2DPoint [this] :- (Seqable Num)))

(ann-record ArrowTrap [dir    :- Kw
                       ready? :- Bool])
(defrecord ArrowTrap [dir
                      ready?])

(ann-record Bat [])
(defrecord Bat [])

(ann-record Broadcaster [name-fn :- Fn])
(defrecord Broadcaster [name-fn])

(ann-record ColossalAmoeba [])
(defrecord ColossalAmoeba [])

(ann-record Counter [turn :- Num])
(defrecord Counter [turn])

(ann-record Digger [can-dig?-fn :- Fn
                    dig-fn      :- Fn])
(defrecord Digger [can-dig?-fn
                   dig-fn])

(ann-record Energy [energy :- Num])
(defrecord Energy [energy]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record Drake [])
(defrecord Drake [])

(ann-record Door [])
(defrecord Door [])

(ann-record Entity
            [id   :- Kw
             type :- Kw])
(defrecord Entity [id
                   type])

;;TODO: "any" -> w.e they are
(ann-record Equipment [weapon :- Any
                       armor  :- Any])
(defrecord Equipment [weapon
                      armor]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record Experience [experience  :- Num
                        level       :- Num
                        level-up-fn :- [UUID Map -> Map]])
(defrecord Experience [experience
                       level
                       level-up-fn]
  ISaveState
  (->save-state [this]
    (let [this (dissoc this :level-up-fn)]
      (zipmap (keys this) (vals this)))))

(ann-record Fireball [])
(defrecord Fireball [])

(ann-record GiantAmoeba [])
(defrecord GiantAmoeba [])

(ann-record Gold [value :- Num])
(defrecord Gold [value])

(ann-record HydraHead [])
(defrecord HydraHead [])

(ann-record HydraNeck [])
(defrecord HydraNeck [])

(ann-record HydraTail [])
(defrecord HydraTail [])

(ann-record HydraRear [])
(defrecord HydraRear [])

(ann-record Inspectable [msg :- String])
(defrecord Inspectable [msg])

(ann-record Inventory [slot :- Equipment
                       junk :- (Seqable Equipment)
                       hp-potion :- Num
                       mp-potion :- Num])
(defrecord Inventory [slot
                      junk
                      hp-potion
                      mp-potion]
  ISaveState
  (->save-state [this]
    (let [saved-slot (if slot
                       (->save-state slot)
                       nil)]
      {:junk (map ->save-state junk)
       :hp-potion hp-potion
       :slot saved-slot})))

(ann-record Item [pickup-fn :- Fn])
(defrecord Item [pickup-fn])

(ann-record Killable [experience :- Num])
(defrecord Killable [experience])

(ann-record Klass [class :- Kw])
(defrecord Klass [class]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record Lichen [grow-chance%  :- Num
                    max-blob-size :- Num])
(defrecord Lichen [grow-chance%
                   max-blob-size])

(ann-record Magic [mp     :- Num
                   max-mp :- Num
                   spells :- Any])
(defrecord Magic [mp
                  max-mp
                  spells])

(ann-record LargeAmoeba [])
(defrecord LargeAmoeba [])

(ann-record Mimic [])
(defrecord Mimic [])

(ann-record Necromancer [])
(defrecord Necromancer [])

(ann-record MPortal [x :- Num
                     y :- Num
                     z :- Num])
(defrecord MPortal [x y z])

(ann-record Merchant [])
(defrecord Merchant [])

(ann-record Player [name        :- String
                    fog-of-war? :- Bool])
(defrecord Player [name
                   fog-of-war?]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record PlayerSight [distance     :- Double
                         decline-rate :- Double
                         lower-bound  :- Num
                         upper-bound  :- Num
                         torch-power  :- Double])
(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-power]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record Portal [x :- Num
                    y :- Num
                    z :- Num])
(defrecord Portal [x y z])

(ann-record Position [x    :- Num
                      y    :- Num
                      z    :- Num
                      type :- Kw])
(defrecord Position [x y z
                     type]
  IPoint
  (->3DPoint [this]
    [z x y])
  (->2DPoint [this]
    [x y]))

(ann-record Purchasable [value :- Num])
(defrecord Purchasable [value])

(ann-record Race [race :- Kw])
(defrecord Race [race]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record Receiver [])
(defrecord Receiver [])

(ann-record Relay [static   :- (Seqable Map)
                   blocking :- (Seqable Map)])
(defrecord Relay [static
                  blocking])

(ann-record Sight [distance :- Num])
(defrecord Sight [distance])

(ann-record Skeleton [])
(defrecord Skeleton [])

(ann-record Slime [])
(defrecord Slime [])

(ann-record Snake [])
(defrecord Snake [])

(ann-record Spider [])
(defrecord Spider [])

(ann-record Troll [])
(defrecord Troll [])

(ann-record SpikeTrap [visible? :- Bool])
(defrecord SpikeTrap [visible?])

(ann-record Tile [x        :- Num
                  y        :- Num
                  z        :- Num
                  entities :- (Seqable Entity)])
(defrecord Tile [x y z
                 entities])

(ann-record Torch
            [brightness :- Num])
(defrecord Torch [brightness])

(ann-record Trap [])
(defrecord Trap [])

(ann-record Wallet [gold :- Num])
(defrecord Wallet [gold]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(ann-record WillowWisp [])
(defrecord WillowWisp [])

(ann-record World [levels :- (Seqable Tile)
                   add-level-fn :- [ -> Map]])
(defrecord World [levels
                  add-level-fn])

(t/defprotocol IAttacker
  (can-attack? [this
                e-this :- UUID
                e-target :- UUID
                system :- Map] :- Bool)
  (attack      [this
                e-this :- UUID
                e-target :- UUID
                system :- Map] :- Map))
(ann-record Attacker [atk :- Num
                      status-effects :- Map
                      attack-fn :- [Attacker UUID UUID Map -> Map]
                      can-attack?-fn :- [Attacker UUID UUID Map -> Bool]
                      is-valid-target? :- Set])
(defrecord Attacker [atk
                     status-effects
                     attack-fn
                     can-attack?-fn
                     is-valid-target?]
  ISaveState
  (->save-state [this]
    {:atk (:atk this)})
  IAttacker
  (can-attack?     [this e-this e-target system]
    (can-attack?-fn this e-this e-target system))
  (attack     [this e-this e-target system]
    (attack-fn this e-this e-target system)))

(t/defprotocol IDestructible
  (take-damage [this
                e-this :- UUID
                damage :- Num
                e-from :- UUID
                system :- Map] :- Map))
(ann-record Destructible [hp :- Num
                          max-hp :- Num
                          def :- Num
                          status-effects :- Map
                          can-retaliate? :- Bool
                          take-damage-fn :- [Destructible UUID Num UUID Map -> Map]
                          on-death-fn :- [Destructible UUID Num UUID Map -> Map]])
(defrecord Destructible [hp
                         max-hp
                         def
                         status-effects
                         can-retaliate?
                         take-damage-fn
                         on-death-fn]
  ISaveState
  (->save-state [this]
    (let [this (dissoc this :take-damage-fn)]
      (zipmap (keys this) (vals this))))
  IDestructible
  (take-damage     [this e-this damage e-from system]
    (take-damage-fn this e-this damage e-from system)))

(t/defprotocol IMobile
  (can-move? [this
              e-this :- UUID
              target-tile :- Tile
              system :- Map] :- Bool)
  (move      [this
              e-this :- UUID
              target-tile :- Tile
              system :- Map] :- Map))
(ann-record Mobile [can-move?-fn :- [Mobile UUID Tile Map -> Bool]
                    move-fn      :- [Mobile UUID Tile Map -> Map]])
(defrecord Mobile [can-move?-fn
                   move-fn]
  IMobile
  (can-move?     [this e-this target-tile system]
    (can-move?-fn this e-this target-tile system))
  (move     [this e-this target-tile system]
    (move-fn this e-this target-tile system)))

(t/defprotocol IRenderable
  (render [this
           e-this :- UUID
           args   :- Map
           system :- Map] :- Map))
(ann-record Renderable [render-fn :- [Renderable UUID Map Map -> Map]
                        args      :- Map])
(defrecord Renderable [render-fn
                       args]
  IRenderable
  (render     [this e-this argz system]
    (render-fn this e-this argz system)))

(t/defprotocol ITickable
  (tick [this
         e-this :- UUID
         system :- Map] :- Map))
(ann-record Tickable [tick-fn :- [Tickable UUID Map -> Map]
                      pri     :- Num])
(defrecord Tickable [tick-fn pri]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))

(t/tc-ignore
  (def k-comp->type
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
     :world            (type (->World nil nil))}))
