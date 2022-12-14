pluginManagement {
  plugins {
    id("dev.clojurephant.clojure") version("0.7.0-rc.1")
    id("org.ajoberstar.reckon") version("0.16.1")    
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      name = "Clojars"
      url = uri("https://repo.clojars.org/")
    }
    maven {
      name = "Gradle Libs"
      url = uri("https://repo.gradle.org/gradle/libs-releases")
      content {
        includeGroup("org.gradle")
      }
    }
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "clojurephant-tooling"
    
