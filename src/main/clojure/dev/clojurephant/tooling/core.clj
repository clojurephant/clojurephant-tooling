(ns dev.clojurephant.tooling.core
  "The core interaction with Gradle's tooling API is all in
  this namespace. Generally, this shouldn't be used directly
  by users."
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [dev.clojurephant.tooling.impl.event :as event])
  (:import [org.gradle.tooling GradleConnector ResultHandler]
           [org.gradle.tooling.events ProgressListener]
           [dev.clojurephant.plugin.common ClojurephantModel]))

(defn connect
  "Connects to a Gradle project in DIR.
  Returns a ProjectConnection."
  [dir]
  (-> (GradleConnector/newConnector)
      (.forProjectDirectory (io/file dir))
      (.connect)))

(defn tap-listener
  "A progress listener that sends all events
  to Clojure's tap facility."
  []
  (let [cache (atom {})]
    (reify ProgressListener
      (statusChanged [this event]
        (println (event/event event cache))))))

(defn collecting-listener
  "A progress listener that collects all events
  into an atom."
  [db]
  (let [cache (atom {})]
    (reify ProgressListener
      (statusChanged [this event]
        (swap! db conj (event/event event cache))))))

(defn handler
  "A result handler that sends the result to
  DONE (a promise) including DB of events."
  [done db]
  (reify ResultHandler
    (onComplete [this result]
      (deliver done {:result :success
                     :value result
                     :events @db}))
    (onFailure [this failure]
      (deliver done {:result :failed
                     :error failure
                     :events @db}))))

(defn build
  "Runs TASKS against the project CON. Returns a map that can
  be used to either wait for the build to finish or cancel it."
  [con & tasks]
  (let [cancel-source (GradleConnector/newCancellationTokenSource)
        done (promise)
        db (atom [])]
    (-> (.newBuild con)
        (.forTasks (into-array String tasks))
        ;(.setJavaHome (io/file "/usr/lib/jvm/java-18-openjdk-amd64"))
        (.setStandardOutput System/out)
        (.setStandardError System/err)
        (.addProgressListener (collecting-listener db))
        (.withCancellationToken (.token cancel-source))
        (.run (handler done db)))
    {:result done
     :cancel cancel-source}))

(defn wait
  "Waits for a RUN started with (build ...) to finish and
  returns the build result."
  [run]
  @(:result run))

(defn cancel
  "Cancels a RUN started with (build ...)."
  [run]
  (.cancel (:cancel run))
  run)

(defn task-results
  "Returns the RUN's task results. If STATE-FILTER is
  provided you can filter on the task's :state field."
  ([run]
   (task-results run any?))
  ([run state-filter]
   (let [result (wait run)
         task-finish? (fn [e]
                        (and (= :task (get-in e [:operation :descriptor]))
                             (= :finish (:state e))))
         simplify (fn [t]
                    (merge
                     {:task (-> t :operation :name)}
                     (:result t)))]
     (->> (:events result)
          (filter task-finish?)
          (map simplify)
          (filter (fn [t]
                    (state-filter (:result t))))))))

(defn model
  "Gets a model of TYPE from project CON. Returns a map that
  can be used to either wait for the operation to finish or
  cancel it."
  [con type]
  (let [cancel-source (GradleConnector/newCancellationTokenSource)
        done (promise)
        db (atom [])]
    (-> (.model con type)
        ;(.setJavaHome (io/file "/usr/lib/jvm/java-18-openjdk-amd64"))
        (.setStandardOutput System/out)
        (.setStandardError System/err)
        (.withCancellationToken (.token cancel-source))
        (.get (handler done db)))
    {:result done
     :cancel cancel-source}))

(defn clojurephant-model
  "Gets the Clojurephant model. Returns a map that can be
  used to either wiat for the operation to finish or cancel it."
  [con]
  (model con ClojurephantModel))
