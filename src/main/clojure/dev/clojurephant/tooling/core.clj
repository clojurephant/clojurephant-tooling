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

(defn tap-listener []
  (reify ProgressListener
    (statusChanged [this event]
      (println (event/event event)))))

(defn collecting-listener [db]
  (reify ProgressListener
    (statusChanged [this event]
      (swap! db conj (event/event event)))))

(defn handler [done db]
  (reify ResultHandler
    (onComplete [this result]
      (deliver done {:result :success
                     :value result
                     :events @db}))
    (onFailure [this failure]
      (deliver done {:result :failed
                     :error failure
                     :events @db}))))

(defn run [con launcher]
  (let [cancel-source (GradleConnector/newCancellationTokenSource)
        done (promise)
        db (atom [])]
    (-> launcher
        #_(.addProgressListener (tap-listener))
        (.addProgressListener (collecting-listener db))
        (.withCancellationToken (.token cancel-source))
        (.run (handler done db)))
    done))

(defn build [con & tasks]
  (let [launcher (-> (.newBuild con)
                     (.forTasks (into-array String tasks))
                     (.setJavaHome (io/file "/usr/lib/jvm/java-18-openjdk-amd64"))
                     (.setStandardOutput System/out)
                     (.setStandardError System/err))]
    (run con launcher)))

