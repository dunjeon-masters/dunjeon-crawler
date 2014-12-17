(defproject rouje-like "0.0.1-SNAPSHOT"
  :description "FIXME: write description"

  :plugins [[cider/cider-nrepl "0.8.0-SNAPSHOT"]]
  :dependencies [[com.badlogicgames.gdx/gdx "1.3.1"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.3.1"]
                 [com.badlogicgames.gdx/gdx-box2d "1.3.1"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.3.1"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.3.1"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.3.1"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.3.1"
                  :classifier "natives-desktop"]

                 [org.clojure/clojure "1.6.0"]

                 [play-clj "0.3.11"]

                 [prismatic/schema "0.3.3"]
                 [org.clojure/tools.trace "0.7.8"]
                 [org.clojure/math.numeric-tower "0.0.4"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :source-paths ["src" "src-common" ]

  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [rouje-like.core.desktop-launcher]
  :main rouje-like.core.desktop-launcher)
