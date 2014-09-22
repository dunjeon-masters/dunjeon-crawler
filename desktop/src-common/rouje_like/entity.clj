(ns rouje-like.entity
  (:import [clojure.lang Keyword])
  (:require [brute.entity :as br.e]

            [rouje-like.components :as rj.c]))

(defn all-e-with-c
  [system ^Keyword component]
  (br.e/get-all-entities-with-component system (rj.c/get-type component)))

(defn get-c-on-e
  [system entity ^Keyword component]
  (br.e/get-component system entity (rj.c/get-type component)))

(defn add-e
  [system entity]
  (br.e/add-entity system entity))

(defn add-c
  [system entity c-instance]
  (br.e/add-component system entity c-instance))

(defn upd-c
  ([system entity ^Keyword component fn]
   (br.e/update-component system entity (rj.c/get-type component) fn))
  ([system entity ^Keyword component fn & args]
   (br.e/update-component system entity (rj.c/get-type component) fn args)))

(defn kill-e
  [system entity]
  (br.e/kill-entity system entity))

