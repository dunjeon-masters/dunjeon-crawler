(ns rouje-like.magic
  (require [rouje-like.utils :as rj.u :refer [?]]
           [rouje-like.equipment :as rj.eq]
           [rouje-like.config :as rj.cfg]
           [rouje-like.entity-wrapper :as rj.e]
           [rouje-like.components :as rj.c]
           [rouje-like.status-effects :as rj.stef]
           [brute.entity :as br.e]
           [rouje-like.destructible :as rj.d]))

(defn dec-mp
  [system e-this spell]
  (let [c-magic (rj.e/get-c-on-e system e-this :magic)
        mp (:mp c-magic)]
    (rj.e/upd-c system e-this :magic
                (fn [c-magic]
                  (update-in c-magic [:mp]
                             #(- % (spell rj.cfg/spell->mp-cost)))))))

(defn get-first-e-in-range
  [system distance direction world player-pos]
  (let [pos (rj.u/coords+offset player-pos (rj.u/direction->offset direction))]
    (loop [distance distance
           pos pos]
      (let [tile (get-in world pos nil)
            top-e (:id (rj.u/tile->top-entity tile))]
        (if (not (pos? distance))
          nil
          (if (rj.e/get-c-on-e system top-e :destructible)
            top-e
            (recur (dec distance) (rj.u/coords+offset pos (rj.u/direction->offset direction)))))))))

(defn use-fireball
  [system e-this direction]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        e-this-pos  [(:x c-position) (:y c-position)]
        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        world (nth levels (:z c-position))
        spell (:fireball rj.cfg/spell-effects)
        distance (:distance spell)
        damage (:value spell)
        c-magic (rj.e/get-c-on-e system e-this :magic)
        mp (:mp c-magic)]
    ;; TODO lichen when attacked with fireball applies poison still. (Retaliation?)
    ;; TODO status effects on player do not apply when casting a spell
    ;; (ex: poison does not damage player if they stand still and spam fireball)
    ;; TODO add a way to take in another keyboard input to tell the direction of fireball.
    ;; TODO add messages to fireball. ("player shoots fireball [up|down|right|left]" "fireball hits [e-this]" "fireball doesn't hit anything")

    (if (pos? mp)
      (as-> (dec-mp system e-this :fireball) system
            (if-let [e-target (get-first-e-in-range system distance direction world e-this-pos)]
              (as-> (rj.c/take-damage (rj.e/get-c-on-e system e-target :destructible) e-target damage e-this system) system
                    (let [e-fireball (br.e/create-entity)]
                      (rj.e/system<<components
                        system e-fireball
                        [[:fireball {}]
                         [:attacker {:status-effects [(assoc (:fireball rj.cfg/status-effects)
                                                             :e-from e-this
                                                             :apply-fn rj.stef/apply-burn)]}]]))
                    (let [e-fireball (first (rj.e/all-e-with-c system :fireball))]
                      (as-> (rj.d/add-effects system e-target e-fireball) system
                            (rj.e/kill-e system e-fireball))))
              system))
      system)))