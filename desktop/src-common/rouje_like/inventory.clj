(ns rouje-like.inventory
  (:require [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.equipment :as rj.eq]))

(defn add-junk [system e-this]
  "Increment the amount of junk E-THIS is carrying."
  (rj.e/upd-c system e-this :inventory
              (fn [inv-comp]
                (update-in inv-comp [:junk] inc))))

(defn switch-slot-item [system e-this & [item]]
  "Switches ITEM into the inventory slot of E-THIS or removes the current item 
   there if ITEM is not passed."
  (rj.e/upd-c system e-this :inventory
              (fn [inv-comp]
                (assoc-in inv-comp [:slot] item))))

(defn pickup-slot-item [system e-this item]
  "Puts ITEM in E-THIS's inventory slot and adds the old item to E-THIS's
   junk pile if there was one."
  (let [old-slot (:slot (rj.e/get-c-on-e system e-this :inventory))]
    (if old-slot
      (-> system
          (add-junk e-this)
          (switch-slot-item e-this item))
      (switch-slot-item system e-this item))))

(defn equip-slot-item [system e-this]
  "Equip the item E-THIS is currently holding in their inventory slot."
  (let [item (:slot (rj.e/get-c-on-e system e-this :inventory))]
    (if item
      (do (-> system
              (rj.eq/switch-equipment e-this item)
              (add-junk e-this) ; need to check if existing item is equipped
              (switch-slot-item e-this)))
      system)))
