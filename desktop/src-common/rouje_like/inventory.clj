(ns rouje-like.inventory
  (:require [rouje-like.utils :refer [?]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.equipment :as rj.eq]
            [rouje-like.status-effects :as rj.stef]))

(defn add-junk
  [system e-this item]
  "Increment the amount of junk E-THIS is carrying."
  (rj.e/upd-c system e-this :inventory
              (fn [inv-comp]
                (update-in inv-comp [:junk]
                           conj item))))

(defn switch-slot-item
  [system e-this & [item]]
  "Switches ITEM into the inventory slot of E-THIS or removes the current item
   there if ITEM is not passed."
  (rj.e/upd-c system e-this :inventory
              (fn [inv-comp]
                (assoc-in inv-comp [:slot] item))))

(defn pickup-slot-item
  [system e-this item]
  "Puts ITEM in E-THIS's inventory slot and adds the old item to E-THIS's
   junk pile if there was one."
  (let [old-slot (:slot (rj.e/get-c-on-e system e-this :inventory))]
    (if old-slot
      (-> system
          (add-junk e-this item)
          (switch-slot-item e-this item))
      (switch-slot-item system e-this item))))

(defn equip-slot-item
  [system e-this]
  "Equip the item E-THIS is currently holding in their inventory slot."
  (let [slot (:slot (rj.e/get-c-on-e system e-this :inventory))
        item (or (:weapon slot) (:armor slot))
        c-equip (rj.e/get-c-on-e system e-this :equipment)]
    (if item
      (do (as-> system system
              (rj.eq/switch-equipment system e-this item)
              (rj.e/upd-c system e-this :attacker
                          (fn [c-attacker]
                            (update-in c-attacker [:status-effects]
                                       (fn [status-effects]
                                         (map #(assoc % :apply-fn ((:type %) rj.stef/effect-type->apply-fn)) status-effects)))))
              (if ((:type item) c-equip)
                  (add-junk system e-this item)
                  system)
              (switch-slot-item system e-this)))
      system)))
