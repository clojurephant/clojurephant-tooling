(ns dev.clojurephant.tooling.api
  (:require [dev.clojurephant.tooling.core :as core]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(defonce db (atom {}))

(defn connect! [project-dir]
  (swap! db (fn [{:keys [dir connection] :as current-db}]
              (cond
                (nil? connection)
                (merge current-db
                       {:dir project-dir
                        :connection (core/connect project-dir)})

                (= dir project-dir)
                current-db

                :else
                (throw (ex-info "Must close existing connection" {:dir dir})))))
  nil)

(defn close! []
  (swap! db (fn [{:keys [connection]}]
              (when connection
                (.close connection))
              nil))
  nil)

(defn reload-model! []
  (swap! db (fn [current-db]
              (if-let [con (:connection current-db)]
                (let [m (core/wait (core/clojurephant-model con))]
                  (if (= :success (:result m))
                    (assoc current-db :model (edn/read-string (.getEdn (:value m))))
                    (throw (ex-info "Failed to get Clojurephant model"
                                    (select-keys m [:error])))))
                (throw (ex-info "Cannot get model until connect" {}))))))

(defn repl-output-dir []
  (-> @db :model :repl :dir))

(defn ^:private reroot-cljs-build [build new-dir]
  (let [old-dir (:output-dir build)
        replacer (fn [dir]
                   (cond
                     (nil? dir) nil
                     (boolean? dir) dir
                     :else (-> dir
                               (string/replace old-dir new-dir)
                               (string/replace (str (System/getProperty "user.dir") "/") ""))))]
    (-> (:compiler build)
        (update-in [:output-dir] replacer)
        (update-in [:output-to] replacer)
        (update-in [:source-map] replacer))))

(defn cljs-all-build-opts []
  (let [m (:model @db)
        repl-dir (repl-output-dir)
        builds (:clojurescript m)]
    (into {} (map (fn [[id build]]
                 [id (reroot-cljs-build build repl-dir)]))
          builds)))

(defn cljs-build-opts [id]
  (get (cljs-all-build-opts) id))
