(ns rouje-like.t-snake
  (:use [midje.sweet]
        [rouje-like.test-utils])
  (:require [brute.entity :as br.e]
            [rouje-like.components :as rj.c :refer [can-move? move]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u]
            [rouje-like.destructible :as rj.d]
            [rouje-like.mobile :as rj.m]
            [rouje-like.config :as rj.cfg]
            [rouje-like.core :as rj.core]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.snake :as rj.sn]))

(let [system (start)]
  (fact "add-snake"
        (as-> system system
              (:system (rj.sn/add-snake {:system system :z 1}))
              (nil? (rj.e/get-c-on-e system (first (rj.e/all-e-with-c system :snake)) :position)))
        => false))
