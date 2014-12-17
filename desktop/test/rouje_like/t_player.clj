(ns rouje-like.t-player
  (:use [midje.sweet]
        [rouje-like.test-utils]
        [rouje-like.player])
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(let [system (-> (init)
                 (init-player {}))
      e-player (first (rj.e/all-e-with-c system :player))]
  (fact "add-player"
        (rj.e/get-c-on-e system e-player :position)
        => (contains {:type :player})

        (rj.e/all-c-on-e system e-player)
        => (has some :energy)

        (rj.e/get-c-on-e system e-player :energy)
        => (contains {:energy 1})))
