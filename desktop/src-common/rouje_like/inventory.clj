(ns rouje-like.inventory
  (:require [rouje-like.utils :refer [? update-gold]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.equipment :as rj.eq]
            [rouje-like.status-effects :as rj.stef]))

(defn update-junk
  [system e-this upd-fn]
  "Update the junk on E-THIS using UPD-FN. UPD-FN is a function that
   takes the current junk on E-THIS and returns a value that replaces it."
  (rj.e/upd-c system e-this :inventory
              (fn [c-inv]
                (update-in c-inv [:junk]
                           upd-fn))))

(defn add-junk
  [system e-this item]
  "Add ITEM to E-THIS's junk pile."
  (update-junk system e-this
               (fn [junk]
                 (conj junk item))))

(defn junk-value
  [system e-this]
  "Return the value of the junk E-THIS carries."
  (let [c-inv (rj.e/get-c-on-e system e-this :inventory)
        junk (:junk c-inv)]
    (reduce (fn [v i]
              (+ v (rj.eq/equipment-value i)))
            0 junk)))

(defn sell-junk
  [system e-this]
  "Sell the junk E-THIS carries and add it to their wallet."
  (let [junk-value (junk-value system e-this)]
    (as-> system system
          (update-junk system e-this (fn [junk] nil))
          (update-gold system e-this junk-value))))

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
          (add-junk e-this old-slot)
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
