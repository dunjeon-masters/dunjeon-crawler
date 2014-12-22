(ns rouje-like.entity-wrapper
  (:import [java.lang String]
           [java.util UUID])
  (:require [brute.entity :as br.e]
            [clojure.core.typed :as t
             :refer [ann Seqable Any Kw Fn Map]]
            [rouje-like.components :as rj.c]
            [clojure.string :as s]))

(ann ^:no-check rouje-like.components/k-comp->type
     [Kw -> Class])
(ann ^:no-check brute.entity/get-all-entities-with-component
     [Map Class -> Map])
(ann all-e-with-c
     [Map Kw -> Map])
(defn all-e-with-c
  [system component]
  (br.e/get-all-entities-with-component system (rj.c/k-comp->type component)))

(ann ^:no-check brute.entity/get-component
     [Map UUID Class -> Map])
(ann get-c-on-e
     [Map UUID Kw -> Map])
(defn get-c-on-e
  [system entity component]
  (br.e/get-component system entity (rj.c/k-comp->type component)))

(ann ^:no-check brute.entity/add-entity
     [Map UUID -> Map])
(ann add-e
     [Map UUID -> Map])
(defn add-e
  [system entity]
  (br.e/add-entity system entity))

(ann ^:no-check brute.entity/add-component
     [Map UUID Class -> Map])
(ann add-c
     [Map UUID Class -> Map])
(defn add-c
  [system entity c-instance]
  (br.e/add-component system entity c-instance))

(ann ^:no-check brute.entity/update-component
     [Map UUID Class Fn -> Map])
(ann upd-c
     [Map UUID Kw Fn -> Map])
(defn upd-c
  [system entity component -fn-]
  (br.e/update-component system entity (rj.c/k-comp->type component) -fn-))

(ann ^:no-check brute.entity/kill-entity
     [Map UUID -> Map])
(ann kill-e
     [Map UUID -> Map])
(defn kill-e
  [system entity]
  (br.e/kill-entity system entity))

(ann ^:no-check brute.entity/get-all-components-on-entity
     [Map UUID -> Map])
(ann all-c-on-e
     [Map UUID -> Map])
(defn all-c-on-e
  [system entity]
  (br.e/get-all-components-on-entity system entity))

(defn ^:no-check ->CamelCase
  [k]
  (str (s/upper-case (first k))
       (s/replace (apply str (rest k)) #"-(\w)" #(.toUpperCase ^String (%1 1)))))

(defmacro keyword->new-component
  [k#]
  (symbol "rouje-like.components" (str "map->" (->CamelCase (name k#)))))

(defmacro partition->add-c
  [s# e-this# k-component# m-component#]
  `(add-c ~s# ~e-this# ((keyword->new-component ~k-component#) ~m-component#)))

(defmacro system<<components
  [s e-this partitions]
  `(let [s# ~s]
     (as-> s# ~'s
       ~@(for [p partitions]
           `(partition->add-c ~'s ~e-this ~(p 0) ~(p 1))))))

