(ns dev.clojurephant.tooling.figwheel-repl
  "An API to start a ClojureScript REPL via Figwheel
  REPL. This only provides the websocket-based REPL,
  not the full features of Figwheel Main.

  Users may prefer this if they don't want a hot
  reload workflow or just want fewer moving pices
  in their process."
  (:require [dev.clojurephant.tooling.api :as api]
            [dev.clojurephant.tooling.cljs :as cljs]
            [cider.piggieback :as piggie]
            [figwheel.repl :as repl]))

(defn repl-env
  "Creates a new repl environment for build ID.
  Uses the build configuration from Gradle. Presumes
  (api/reload-model!) has been called beforehand."
  [id]
  (let [opts (merge (api/cljs-build-opts id)
                    (api/figwheel-opts id))
        env (repl/repl-env* opts)]
      (cljs/repl-env! id env)))

(defn cljs-repl
  "Starts a ClojureScript REPL using
  Figwheel REPL and Piggieback."
  [id]
  (piggie/cljs-repl (repl-env id)))

(defn start
  "Convenience function to start a ClojureScript
  REPL with one call. Will connect to and get model
  config from Gradle, pre-build the ClojureScript
  then start the REPL."
  ([] (start :dev))
  ([id]
   (api/connect! ".")
   (api/reload-model!)
   (cljs/build! id)
   (cljs-repl id)))
