(ns rouje-like.attacker
  (:require [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.components :as rj.c]
            [rouje-like.destructible :as rj.d]))

#_(in-ns 'rouje-like.attacker)
#_(use 'rouje-like.attacker :reload)

(defn can-attack?
  [{:keys [is-valid-target?]} _ e-target system]
  (let [target-type (:type (rj.e/get-c-on-e system e-target :position))]
    (and (rj.e/get-c-on-e system e-target :destructible)
         (is-valid-target? target-type))))

(defn attack
  [c-this e-this e-target system]
  (let [damage (:atk c-this)

        c-destr (rj.e/get-c-on-e system e-target :destructible)]
    (rj.c/take-damage c-destr e-target damage e-this system)))

