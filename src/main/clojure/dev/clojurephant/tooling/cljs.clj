(ns dev.clojurephant.tooling.cljs
  (:require [dev.clojurephant.tooling.api :as api]
            [cljs.analyzer.api :as ana]
            [cljs.build.api :as build]
            [cljs.closure :as closure]
            [cljs.repl :as repl]))

(def registry (atom {}))

(defn build-opts [id]
  (api/cljs-build-opts id))

(defn compiler-env! [id]
  (let [opts (build-opts id)]
    (-> registry
        (swap! update-in [id :compiler-env]
               (fn [env] (or env (ana/empty-state opts))))
        (get-in [id :compiler-env]))))

(defn build! [id]
  (let [opts (build-opts id)
        compiler-env (compiler-env! id)]
    (build/build nil opts compiler-env)))

(defn watch! [id]
  (let [opts (build-opts id)
        compiler-env (compiler-env! id)
        stop (promise)]
    (swap! registry assoc-in [id :watch-stop] stop)
    (build/watch nil opts compiler-env stop)))

(defn stop-watch! [id]
  (swap! registry update-in [id :watch-stop]
         (fn [stop] (when stop (deliver stop true))))
  nil)

(defn clean! [id]
  (let [opts (build-opts id)]
    (stop-watch! id)
    (swap! registry dissoc id)
    ;; TODO delete output-dir and output-to
    ))

(defn repl-env!
  ([id]
   (get-in registry [id :repl-env]))
  ([id env]
   (swap! registry update-in [id :repl-env]
          (fn [old-env]
            (when old-env
              (repl/tear-down old-env))
            env))
   env))
