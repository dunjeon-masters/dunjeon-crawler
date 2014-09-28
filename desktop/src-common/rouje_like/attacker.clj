(ns rouje-like.attacker
  (:require [rouje-like.utils :as rj.u]
            [rouje-like.entity :as rj.e]
            [rouje-like.components :as rj.c]))

(defn can-attack?
  [_ _ e-target system]
  (not (nil? (rj.e/get-c-on-e system e-target :destructible))))

(defn attack
  [_ e-this e-target system]
  (let [damage (:atk (rj.e/get-c-on-e system e-this :attacker))

        c-destr (rj.e/get-c-on-e system e-target :destructible)]
    (rj.c/take-damage c-destr e-target damage e-this system)))
