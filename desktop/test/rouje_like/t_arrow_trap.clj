(ns rouje-like.t-arrow-trap
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.arrow-trap])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(let [system (start)]
  (fact "add-trap"
        (as-> system system
              (:system (add-trap {:system system :z 1}))
              (nil? (rj.e/get-c-on-e system (first (rj.e/all-e-with-c system :arrow-trap)) :position)))
        => false))
