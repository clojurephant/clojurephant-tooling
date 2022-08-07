(ns dev.clojurephant.tooling.cljs
  "An API to run ClojureScript builds using configuration
  from the Gradle build within the REPL. These functions
  use the ClojureScript build API."
  (:require [dev.clojurephant.tooling.api :as api]
            [cljs.analyzer.api :as ana]
            [cljs.build.api :as build]
            [cljs.closure :as closure]
            [cljs.repl :as repl]))

(def registry (atom {}))

(defn build-opts
  "Returns a map of compiler options for the build ID."
  [id]
  (api/cljs-build-opts id))

(defn compiler-env!
  "Gets the compiler environment for build ID
  or sets it to an empty state, if none exists
  in REGISTRY."
  [id]
  (let [opts (build-opts id)]
    (-> registry
        (swap! update-in [id :compiler-env]
               (fn [env] (or env (ana/empty-state opts))))
        (get-in [id :compiler-env]))))

(defn build!
  "Runs build ID once."
  [id]
  (let [opts (build-opts id)
        compiler-env (compiler-env! id)]
    (build/build nil opts compiler-env)))

(defn watch!
  "Starts build ID in watch mode. Can be stopped
  with (stop-watch! id)."
  [id]
  (let [opts (build-opts id)
        compiler-env (compiler-env! id)
        stop (promise)]
    (swap! registry assoc-in [id :watch-stop] stop)
    (build/watch nil opts compiler-env stop)))

(defn stop-watch!
  "Stops build ID's watch."
  [id]
  (swap! registry update-in [id :watch-stop]
         (fn [stop] (when stop (deliver stop true))))
  nil)

(defn clean!
  "Cleans build ID. Stops watch, and blows away details
  in REGISTRY."
  [id]
  (let [opts (build-opts id)]
    (stop-watch! id)
    (swap! registry dissoc id)
    ;; TODO delete output-dir and output-to
    ))

(defn repl-env!
  "Gets or sets the repl environment for build ID.
  With one argument -- the ID -- gets an existing
  repl env. With two arguments -- the ID and env
  -- sets the repl env. If build ID already has
  a repl env, tears it down before replacing it
  in REGISTRY."
  ([id]
   (get-in @registry [id :repl-env]))
  ([id env]
   (swap! registry update-in [id :repl-env]
          (fn [old-env]
            (when old-env
              (repl/tear-down old-env))
            env))
   env))
