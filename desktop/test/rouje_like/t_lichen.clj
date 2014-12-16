(ns rouje-like.t-lichen
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.lichen])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(let [system (start)]
  (fact "add-lichen"
        (as-> system system
              (:system (add-lichen {:system system :z 1}))
              (nil? (rj.e/get-c-on-e system (first (rj.e/all-e-with-c system :lichen)) :position)))
        => false))


