(ns dev.clojurephant.tooling.repl.browser
  (:require [dev.clojurephant.tooling.api :as api]
            [cljs.build.api :as build]
            [cljs.repl.browser :as browser]
            [cider.piggieback :as piggieback]))

(defn cljs-build [id]
  (let [opts (api/repl-cljs-build id)]
    (build/build opts)))

(defn cljs-repl []
  (let [opts (api/repl-cljs-build :dev)]
    (piggieback/cljs-repl
     (browser/repl-env)
     opts)))
