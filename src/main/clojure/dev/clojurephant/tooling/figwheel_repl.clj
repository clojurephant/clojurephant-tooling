(ns dev.clojurephant.tooling.figwheel-repl
  (:require [dev.clojurephant.tooling.api :as api]
            [dev.clojurephant.tooling.cljs :as cljs]
            [cider.piggieback :as piggie]
            [figwheel.repl :as repl]))

(defn repl-env [id]
  (let [opts (assoc (cljs/build-opts id)
                    :open-url false)
          env (repl/repl-env* opts)]
      (cljs/repl-env! id env)))

(defn cljs-repl [id]
  (piggie/cljs-repl (repl-env id)))

(defn start
  ([] (start :dev))
  ([id]
   (api/connect! ".")
   (api/reload-model!)
   (cljs/build! id)
   (cljs-repl id)))
