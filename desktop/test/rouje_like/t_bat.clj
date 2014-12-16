(ns rouje-like.t-bat
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
            [rouje-like.bat :as rj.bt]))

(let [system (start)]
  (fact "add-bat"
        (as-> system system
              (:system (rj.bt/add-bat {:system system :z 1}))
              (nil? (rj.e/get-c-on-e system (first (rj.e/all-e-with-c system :bat)) :position)))
        => false))
