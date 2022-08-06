(ns dev.clojurephant.tooling.api
  (:require [dev.clojurephant.tooling.core :as core]
            [clojure.string :as string]))

(defonce db (atom {}))

(defn connect [dir]
  (swap! db update :con
         (fn [existing-con]
           (if existing-con
             (throw (ex-info "Must close existing connection" {}))
             (core/connect dir)))))

(defn close []
  (swap! db update :con
         (fn [existing-con]
           (when existing-con
             (.close existing-con)
             nil))))

(defn model []
  (swap! db (fn [current-db]
              (if-let [con (:con current-db)]
                (let [m (core/wait (core/clojurephant-model con))]
                  (if (= :success (:result m))
                    (assoc current-db :model (read-string (.getEdn (:value m))))
                    (throw (ex-info "Failed to get Clojurephant model"
                                    (select-keys m [:error])))))
                (throw (ex-info "Cannot get model until connect" {}))))))

(defn reroot-cljs-build [build new-dir]
  (let [old-dir (:output-dir build)
        replacer (fn [dir]
                   (cond
                     (nil? dir) nil
                     (boolean? dir) dir
                     :else (string/replace dir old-dir new-dir)))]
    (-> (:compiler build)
        (update-in [:output-dir] replacer)
        (update-in [:output-to] replacer)
        (update-in [:source-map] replacer))))

(defn repl-cljs-builds []
  (let [m (:model @db)
        repl-dir (-> m :repl :dir)
        builds (:clojurescript m)]
    (into {} (map (fn [[id build]]
                 [id (reroot-cljs-build build repl-dir)]))
          builds)))

(defn repl-cljs-build [id]
  (get (repl-cljs-builds) id))
