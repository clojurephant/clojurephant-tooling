(ns dev.clojurephant.tooling.impl.event
  (:import [org.gradle.tooling.events OperationDescriptor ProgressListener ProgressEvent StartEvent FinishEvent StatusEvent]
           [org.gradle.tooling.events.download FileDownloadOperationDescriptor FileDownloadFinishEvent FileDownloadProgressEvent FileDownloadStartEvent]
           [org.gradle.tooling.events.configuration ProjectConfigurationOperationDescriptor ProjectConfigurationFinishEvent ProjectConfigurationProgressEvent ProjectConfigurationStartEvent]
           [org.gradle.tooling.events.task TaskOperationDescriptor TaskFinishEvent TaskProgressEvent TaskStartEvent TaskSuccessResult TaskSkippedResult TaskFailureResult]
           [org.gradle.tooling.events.test JvmTestOperationDescriptor TestOperationDescriptor TestOutputDescriptor TestFinishEvent TestOutputEvent TestProgressEvent TestStartEvent TestFailureResult TestSkippedResult TestSuccessResult]
           [org.gradle.tooling.events.transform TransformOperationDescriptor TransformFinishEvent TransformProgressEvent TransformStartEvent]
           [org.gradle.tooling.events.work WorkItemOperationDescriptor WorkItemProgressEvent WorkItemProgressEvent WorkItemStartEvent]))

(defprotocol OperationParser
  (parse-operation [op]))

(extend-protocol OperationParser
  ProjectConfigurationOperationDescriptor
  (parse-operation [op]
    {:descriptor :project-config
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :build-dir (-> op (.getProject) (.getBuildIdentifier) (.getRootDir))
     :project (-> op (.getProject) (.getProjectPath))})

  TaskOperationDescriptor
  (parse-operation [op]
    {:descriptor :task
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :task (-> op (.getTaskPath))
     :plugin (-> op (.getOriginPlugin) (.getDisplayName))
     :dependencies (into [] (map parse-operation) (.getDependencies op))})

  TransformOperationDescriptor
  (parse-operation [op]
    {:descriptor :transform
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :subject (-> op (.getSubject) (.getDisplayName))
     :transformer (-> op (.getTransformer) (.getDisplayName))
     :dependencies (into [] (map parse-operation (.getDependencies op)))})

  WorkItemOperationDescriptor
  (parse-operation [op]
    {:description :work-item
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :class (.getClassName op)})

  FileDownloadOperationDescriptor
  (parse-operation [op]
    {:description :download
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :uri (.getUri op)})

  JvmTestOperationDescriptor
  (parse-operation [op]
    {:descriptor :test
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :test-kind (-> op (.getJvmTestKind) (.getLabel))
     :suite (.getSuiteName op)
     :class (.getClassName op)
     :method (.getMethodName op)})

  TestOutputDescriptor
  (parse-operation [op]
    {:descriptor :test-output
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))
     :destination (-> op (.getDestination) (.name))
     :message (.getMessage op)})

  OperationDescriptor
  (parse-operation [op]
    {:descriptor :generic
     :display-name (.getDisplayName op)
     :name (.getName op)
     :parent (parse-operation (.getParent op))})

  nil
  (parse-operation [op]
    nil))

(defn operation [op]
  (parse-operation op))

(defn base-parse-event [event]
  {:operation (operation (.getDescriptor event))
   :state (cond
            (instance? StartEvent event) :start
            (instance? StatusEvent event) :status
            (instance? FinishEvent event) :finish
            :else :unknown)
   :display-name (.getDisplayName event)
   :time (.getEventTime event)})

(defn task-result [result]
  (cond
    (instance? TaskSuccessResult result)
    {:result :success
     :up-to-date (.isUpToDate result)
     :from-cache (.isFromCache result)}

    (instance? TaskSkippedResult result)
    {:result :skipped
     :reason (.getSkipMessage result)}

    (instance? TaskFailureResult result)
    {:result :failed
     :failures (.getFailures result)}

    :else
    {:result :unknown
     :detail result}))

(defprotocol EventParser
  (parse-event [event]))

(extend-protocol EventParser
  TaskFinishEvent
  (parse-event [event]
    (-> (base-parse-event event)
        (assoc :result (task-result (.getResult event)))))

  TestFinishEvent
  (parse-event [event]
    (-> (base-parse-event event)
        (assoc :result (cond
                         (instance? TestSuccessResult (.getResult event)) :success
                         (instance? TestSkippedResult (.getResult event)) :skipped
                         (instance? TestFailureResult (.getResult event)) :failed))))

  ProgressEvent
  (parse-event [event]
    (base-parse-event event)))

(defn event [e]
  (parse-event e))
