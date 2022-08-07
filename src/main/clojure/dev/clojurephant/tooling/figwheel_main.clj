(ns dev.clojurephant.tooling.figwheel-main
  (:require [dev.clojurephant.tooling.api :as api]
            [dev.clojurephant.tooling.cljs :as cljs]
            [figwheel.main.api :as fig]))

(defn build-map [id]
  {:id (name id)
   :options (api/cljs-build-opts id)
   :config {}})

(defn cljs-repl [id]
  (fig/cljs-repl (name id)))

(defn start
  ([] (start :dev))
  ([id]
   (api/connect! ".")
   (api/reload-model!)
   (fig/start (build-map id))))
