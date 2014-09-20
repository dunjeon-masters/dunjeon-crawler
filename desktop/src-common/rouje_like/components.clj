(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword]))

(def block-size 27)

;; FOR ENTITY E-WORLD:
(defrecord World [^Atom tiles]) ;; 2D Vector of Tile's
(defrecord Tile [^Number x ^Number y ^Keyword type])

;; FOR ENTITY E-PLAYER:
(defrecord Player [^Atom tiles ^Atom show-world?]) ;; 2D Vector of Tile's
(defrecord Digger [^Fn can-dig? ^Fn dig!])
(defrecord Position [^Atom x ^Atom y ^Fn can-move? ^Fn move!])
(defrecord MovesLeft [^Atom moves-left])
(defrecord Gold [^Atom gold])
(defrecord Sight [^Atom distance ^Atom decline-rate
                  ^Atom lower-bound ^Atom upper-bound])

;; FOR EVERYTHING:
(defrecord Renderable [^Number pri ^Fn render-fn args]) ;; args is a map

;; Workaround for not being able to get record's type "statically"
(def get-type {:world      (type (->World nil))
               :tile       (type (->Tile nil nil nil))
               :player     (type (->Player nil nil))
               :digger     (type (->Digger nil nil))
               :position   (type (->Position nil nil nil nil))
               :moves-left (type (->MovesLeft nil))
               :gold       (type (->Gold nil))
               :sight      (type (->Sight nil nil nil nil))
               :renderable (type (->Renderable nil nil nil))})
