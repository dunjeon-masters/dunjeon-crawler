(ns rouje-like.t-portal
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.portal])
  (:require [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c]
            [rouje-like.config :as rj.cfg]
            [rouje-like.core :as rj.core]))

(let [system (start)
      system (:system (add-portal {:system system :z 1}))
      e-world (first (rj.e/all-e-with-c system :world))
      {:keys [levels]} (rj.e/get-c-on-e system e-world :world)
      e-portal (first (rj.e/all-e-with-c system :portal))
      c-portal-pos (rj.e/get-c-on-e system e-portal :position)
      portal-pos (portal-target-pos system c-portal-pos)]
  (fact "add-portal"
        e-portal => truthy)
  (fact "portal-target-pos"
        portal-pos => (rj.c/->3DPoint c-portal-pos))
  (fact "is-portal?"
        (rj.u/tile->top-entity
          (get-in levels (rj.c/->3DPoint c-portal-pos)))
        => (contains {:type :portal})))

