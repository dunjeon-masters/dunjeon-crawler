(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector])
  (:require [schema.core :as s]))

#_(use 'rouje-like.components :reload)

(defprotocol ISaveState
  (->save-state [this]))
(defprotocol IPoint
  (->3DPoint [this])
  (->2DPoint [this]))

(s/defrecord ArrowTrap [dir    :- s/Keyword
                        ready? :- s/Bool])

(s/defrecord Bat [])

(s/defrecord Broadcaster [name-fn :- Fn])

(s/defrecord ColossalAmoeba [])

(s/defrecord Counter [turn :- s/Num])

(s/defrecord Digger [can-dig?-fn :- Fn
                     dig-fn      :- Fn])

(s/defrecord Energy [energy :- s/Num]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord Drake [])

(s/defrecord Door [])


(s/defrecord Entity [id   :- java.util.UUID
                     type :- s/Keyword])

(s/defrecord Equipment [weapon :- s/Any
                        armor  :- s/Any]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord Experience [experience  :- s/Num
                         level       :- s/Num
                         level-up-fn :- Fn]
  ISaveState
  (->save-state [this]
    (let [this (dissoc this :level-up-fn)]
      (zipmap (keys this) (vals this)))))

(s/defrecord Fireball [])

(s/defrecord GiantAmoeba [])

(s/defrecord Gold [value :- s/Num])

(s/defrecord HydraHead [])

(s/defrecord HydraNeck [])

(s/defrecord HydraTail [])

(s/defrecord HydraRear [])

(s/defrecord Inspectable [msg :- [String]])

(s/defrecord Inventory [slot      :- Equipment
                        junk      :- [Equipment]
                        hp-potion :- s/Num
                        mp-potion :- s/Num]
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

(s/defrecord Item [pickup-fn :- Fn])

(s/defrecord Killable [experience])

(s/defrecord Klass [class]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord Lichen [grow-chance%  :- s/Num
                     max-blob-size :- s/Num])

(s/defrecord Magic [mp     :- s/Num
                    max-mp :- s/Num
                    spells :- s/Num])

(s/defrecord LargeAmoeba [])

(s/defrecord Mimic [])

(s/defrecord Necromancer [])

(s/defrecord MPortal [x :- s/Num
                      y :- s/Num
                      z :- s/Num])

(s/defrecord Merchant [])

(s/defrecord Player [name        :- String
                     fog-of-war? :- s/Bool]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord PlayerSight [distance         :- s/Num
                          decline-rate     :- s/Num
                          lower-bound      :- s/Int
                          upper-bound      :- s/Int
                          torch-multiplier :- s/Num]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord Portal [x :- s/Num
                     y :- s/Num
                     z :- s/Num])

(s/defrecord Position [x    :- s/Num
                       y    :- s/Num
                       z    :- s/Num
                       type :- s/Keyword]
  IPoint
  (->3DPoint [this]
    [z x y])
  (->2DPoint [this]
    [x y]))

(s/defrecord Purchasable [value :- s/Num])

(s/defrecord Race [race :- s/Keyword]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord Receiver [])

(s/defrecord Relay [static   :- [s/Any]
                    blocking :- [s/Any]])

(s/defrecord Sight [distance :- s/Num])

(s/defrecord Skeleton [])

(s/defrecord Slime [])

(s/defrecord Snake [])

(s/defrecord Spider [])

(s/defrecord Troll [])

(s/defrecord SpikeTrap [visible? :- s/Bool])

(s/defrecord Tile [x        :- s/Num
                   y        :- s/Num
                   z        :- s/Num
                   entities :- [Entity]]
  IPoint
  (->3DPoint [this]
    [z x y])
  (->2DPoint [this]
    [x y]))

(s/defrecord Torch [brightness :- s/Num])

(s/defrecord Trap [])

(s/defrecord Wallet [gold :- s/Num]
  ISaveState
  (->save-state [this]
    (zipmap (keys this) (vals this))))

(s/defrecord WillowWisp [])

(s/defrecord World [levels       :- [[Tile]]
                    add-level-fn :- Fn])

(defprotocol IAttacker
  (can-attack? [this e-this e-target system])
  (attack      [this e-this e-target system]))
(s/defrecord Attacker [atk              :- s/Num
                       status-effects   :- [s/Any]
                       attack-fn        :- Fn
                       can-attack?-fn   :- Fn
                       is-valid-target? :- Fn]
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
(s/defrecord Destructible [hp             :- s/Num
                           max-hp         :- s/Num
                           def            :- s/Num
                           status-effects :- [s/Any]
                           can-retaliate? :- s/Bool
                           take-damage-fn :- Fn
                           on-death-fn    :- Fn]
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
(s/defrecord Mobile [can-move?-fn :- Fn
                     move-fn      :- Fn]
  IMobile
  (can-move?     [this e-this target-tile system]
    (can-move?-fn this e-this target-tile system))
  (move     [this e-this target-tile system]
    (move-fn this e-this target-tile system)))

(defprotocol IRenderable
  (render [this e-this args system]))
(s/defrecord Renderable [render-fn :- Fn
                         args      :- {s/Keyword s/Any}]
  IRenderable
  (render     [this e-this argz system]
    (render-fn this e-this argz system)))

(defprotocol ITickable
  (tick [this e-this system]))
(s/defrecord Tickable [tick-fn :- Fn
                       pri     :- s/Num]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))

(def get-type
  "Workaround for not being able to get record's type 'statically'"
  {:arrow-trap       (type (map->ArrowTrap {}))
   :attacker         (type (map->Attacker {}))
   :bat              (type (map->Bat {}))
   :broadcaster      (type (map->Broadcaster {}))
   :class            (type (map->Klass {}))
   :colossal-amoeba  (type (map->ColossalAmoeba {}))
   :counter          (type (map->Counter {}))
   :destructible     (type (map->Destructible {}))
   :digger           (type (map->Digger {}))
   :door             (type (map->Door {}))
   :drake            (type (map->Drake {}))
   :energy           (type (map->Energy {}))
   :entity           (type (map->Entity {}))
   :equipment        (type (map->Equipment {}))
   :experience       (type (map->Experience {}))
   :fireball         (type (map->Fireball {}))
   :gold             (type (map->Gold {}))
   :giant-amoeba     (type (map->GiantAmoeba {}))
   :hydra-head       (type (map->HydraHead {}))
   :hydra-neck       (type (map->HydraNeck {}))
   :hydra-tail       (type (map->HydraTail {}))
   :hydra-rear       (type (map->HydraRear {}))
   :inventory        (type (map->Inventory {}))
   :inspectable      (type (map->Inspectable {}))
   :item             (type (map->Item {}))
   :killable         (type (map->Killable {}))
   :large-amoeba     (type (map->LargeAmoeba {}))
   :lichen           (type (map->Lichen {}))
   :magic            (type (map->Magic {}))
   :m-portal         (type (map->MPortal {}))
   :merchant         (type (map->Merchant {}))
   :mobile           (type (map->Mobile {}))
   :mimic            (type (map->Mimic {}))
   :necromancer      (type (map->Necromancer {}))
   :player           (type (map->Player {}))
   :playersight      (type (map->PlayerSight {}))
   :portal           (type (map->Portal {}))
   :position         (type (map->Position {}))
   :purchasable      (type (map->Purchasable {}))
   :race             (type (map->Race {}))
   :receiver         (type (map->Receiver {}))
   :relay            (type (map->Relay {}))
   :renderable       (type (map->Renderable {}))
   :sight            (type (map->Sight {}))
   :skeleton         (type (map->Skeleton {}))
   :slime            (type (map->Slime {}))
   :spider           (type (map->Spider {}))
   :spike-trap       (type (map->SpikeTrap {}))
   :snake            (type (map->Snake {}))
   :tickable         (type (map->Tickable {}))
   :tile             (type (map->Tile {}))
   :torch            (type (map->Torch {}))
   :trap             (type (map->Trap {}))
   :troll            (type (map->Troll {}))
   :wallet           (type (map->Wallet {}))
   :willow-wisp      (type (map->WillowWisp {}))
   :world            (type (map->World {}))})
