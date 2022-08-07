(ns dev.clojurephant.tooling.figwheel-main
  "An API to start a ClojureScript REPL via Figwheel
  Main. This uses configuration from Gradle, rather
  than `<build>.cljs.edn` files.

  As opposed to the `.figwheel-repl` namespace, this
  does provide the full hot reload workflow."
  (:require [dev.clojurephant.tooling.api :as api]
            [dev.clojurephant.tooling.cljs :as cljs]
            [figwheel.main.api :as fig]))

(defn build-map
  "Gets a build map for Figwheel Main usage. Loads
  the build configuration from Gradle. Presumes
  (api/reload-model!) has been called beforehand."
  [id]
  {:id (name id)
   :options (api/cljs-build-opts id)
   :config {}})

(defn cljs-repl
  "Starts a ClojureScript REPL using
  Figwheel Main and Piggieback."
  [id]
  (fig/cljs-repl (name id)))

(defn start
  "Convenience function to start a ClojureScript
  REPL with one call. Will connect to and get model
  config from Gradle, then start the Figwheel Main
  build and REPL."
  ([] (start :dev))
  ([id]
   (api/connect! ".")
   (api/reload-model!)
   (fig/start (build-map id))))
