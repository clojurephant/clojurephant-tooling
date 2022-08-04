(ns dev.clojurephant.tooling.core
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [dev.clojurephant.tooling.impl.event :as event])
  (:import [org.gradle.tooling GradleConnector ResultHandler]
           [org.gradle.tooling.events ProgressListener]))

(defn connect [dir]
  (-> (GradleConnector/newConnector)
      (.forProjectDirectory (io/file dir))
      (.connect)))

(defn listener []
  (reify ProgressListener
    (statusChanged [this event]
      (println {:operation (event/operation (.getDescriptor event))
                :display-name (.getDisplayName event)
                :time (.getEventTime event)}))))

(defn handler [done]
  (reify ResultHandler
    (onComplete [this result]
      (deliver done {:result :success
                     :value result}))
    (onFailure [this failure]
      (deliver done {:result :failed
                     :error failure}))))

(defn run [con launcher]
  (let [cancel-source (GradleConnector/newCancellationTokenSource)
        done (promise)]
    (-> launcher
        (.addProgressListener (listener))
        (.withCancellationToken (.token cancel-source))
        (.run (handler done)))
    done))

(defn build [con & tasks]
  (let [launcher (-> (.newBuild con)
                     (.forTasks (into-array String tasks))
                     (.setJavaHome (io/file "/usr/lib/jvm/java-18-openjdk-amd64"))
                     (.setStandardOutput System/out)
                     (.setStandardError System/err))]
    (run con launcher)))

