(ns rouje-like.t-entity-wrapper
  (:use [midje.sweet]
        [rouje-like.test-utils])
  (:require [rouje-like.entity-wrapper :refer :all]))

(fact "->CamelCase"
      (->CamelCase "my-snake-case-string") => "MySnakeCaseString"
      (->CamelCase "foo") => "Foo")

(fact "keyword->new-component"
      (keyword->new-component :energy)
      =expands-to=> rouje-like.components/strict-map->Energy
      ((keyword->new-component :energy) {:energy 1
                                         :default-energy 1})
      => (contains {:energy 1
                    :default-energy 1}))

(fact "partition->add-c"
      (let [system :system
            e-this :e-this]
        (partition->add-c system e-this
                          :asdf {:foo :bar}))
      =expands-to=> (let* [system :system
                           e-this :e-this]
                      (partition->add-c system e-this
                                        :asdf {:foo :bar})))

(fact "system<<components"
      (let [system :system
            e-this :e-this]
        (system<<components system e-this
                            [[:player {:name "foo"}]
                             [:asdf {:foo :bar}]]))
      =expands-to=> (let* [system :system
                           e-this :e-this]
                      (system<<components system e-this
                                          [[:player {:name "foo"}]
                                           [:asdf {:foo :bar}]])))
