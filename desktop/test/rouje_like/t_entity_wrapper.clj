(ns rouje-like.t-entity-wrapper
  (:use [midje.sweet]
        [rouje-like.test-utils])
  (:require [rouje-like.entity-wrapper :refer :all]))

(fact "->CamelCase"
      (->CamelCase "my-snake-case-string") => "MySnakeCaseString"
      (->CamelCase "foo") => "Foo")

(fact "keyword->new-component"
      (keyword->new-component :asdf)
      =expands-to=> rouje-like.components/map->Asdf)

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
