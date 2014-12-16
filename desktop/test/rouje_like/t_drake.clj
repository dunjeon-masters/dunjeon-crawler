(ns rouje-like.t-drake
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.drake])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(let [system (start)]
  (fact "add-drake"
        (as-> system system
              (:system (add-drake {:system system :z 1}))
              (nil? (rj.e/get-c-on-e system (first (rj.e/all-e-with-c system :drake)) :position)))
        => false))
