(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword]))

;; FOR ENTITY E-BOARD:
(defrecord World [^Atom tiles]) ;; atom - 2D Vector of Tile's
(defrecord Tile [^Number x ^Number y ^Keyword type])

;; FOR ENTITY E-PLAYER:
(defrecord Player [^Atom tiles]) ;; atom - 2D Vector of Tile's
(defrecord Position [^Atom x ^Atom y]) ;; atoms
(defrecord MovesLeft [^Atom moves-left]) ;; atom
(defrecord Score [^Atom score]) ;; atom
(defrecord Sight [^Atom distance]) ;; atom

;; FOR EVERYTHING:
(defrecord Renderable [^Number pri ^Fn fn args])

;; Workaround for not being able to get record's type "statically"
(def get-type {:world      (type (->World nil))
               :tile       (type (->Tile nil nil nil))
               :player     (type (->Player nil))
               :position   (type (->Position nil nil))
               :moves-left (type (->MovesLeft nil))
               :score      (type (->Score nil))
               :sight      (type (->Sight nil))
               :renderable (type (->Renderable nil nil nil))})
