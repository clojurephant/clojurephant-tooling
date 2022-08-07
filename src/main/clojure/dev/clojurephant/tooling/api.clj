(ns dev.clojurephant.tooling.api
  "A higher-level API to interact with Gradle's tooling API.
  Generally, prefer this to use the `dev.clojurephat.tooling.core`
  API."
  (:require [dev.clojurephant.tooling.core :as core]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(defonce db (atom {}))

(defn connect!
  "Connects to a Gradle project in PROJECT-DIR. If DB
  already has a connection for this PROJECT-DIR, do
  nothing. If DB has a connection for another dir, throw.
  Otherwise create a new connection and store in DB."
  [project-dir]
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

(defn close!
  "Closes an existing connection in DB. If there's
  no existing connection, do nothing."
  []
  (swap! db (fn [{:keys [connection]}]
              (when connection
                (.close connection))
              nil))
  nil)

(defn reload-model!
  "Loads the Clojurephant model and stores it in DB. Requires you to
  have called (connect! ...) before. Reloads model from Gradle config
  even if model already populated in DB."
  []
  (swap! db (fn [current-db]
              (if-let [con (:connection current-db)]
                (let [m (core/wait (core/clojurephant-model con))]
                  (if (= :success (:result m))
                    (assoc current-db :model (edn/read-string (.getEdn (:value m))))
                    (throw (ex-info "Failed to get Clojurephant model"
                                    (select-keys m [:error])))))
                (throw (ex-info "Cannot get model until connect" {}))))))

(defn repl-output-dir
  "Gets the repl output directory. Requires calling
  (reload-model!) before."
  []
  (-> @db :model :repl :dir))

(defn ^:private reroot-cljs-build
  "Takes a ClojureScript build and re-evaluates the
  output directories to be relative to the REPL's
  output directory."
  [build new-dir]
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

(defn cljs-all-build-opts
  "Returns a map of all ClojureScript build's compiler options.
  Key is a keyword of the build name, value is a map of compiler
  options."
  []
  (let [m (:model @db)
        repl-dir (repl-output-dir)
        builds (:clojurescript m)]
    (into {} (map (fn [[id build]]
                 [id (reroot-cljs-build build repl-dir)]))
          builds)))

(defn cljs-build-opts
  "Returns a map of compiler options for the build ID."
  [id]
  (get (cljs-all-build-opts) id))
