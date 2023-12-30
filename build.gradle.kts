plugins {
  id("dev.clojurephant.clojure")
  id("java-library")
  id("maven-publish")

  id("org.ajoberstar.reckon")
}

group = "dev.clojurephant"

reckon {
  setDefaultInferredScope("patch")
  stages("alpha", "beta", "rc", "final")
  setScopeCalc(calcScopeFromProp().or(calcScopeFromCommitMessages()))
  setStageCalc(calcStageFromProp())
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
  }
  withSourcesJar()

  registerFeature("figwheelRepl") {
    usingSourceSet(sourceSets["main"])
  }
  registerFeature("figwheelMain") {
    usingSourceSet(sourceSets["main"])
  }
}

dependencies {
  // clojure
  api("org.clojure:clojure:1.11.1")

  // gradle
  compileOnly("org.gradle:gradle-tooling-api:8.5")

  // clojurescript and nrepl
  api("org.clojure:clojurescript:1.11.121")
  api("nrepl:nrepl:1.1.0")
  api("cider:piggieback:0.5.3")

  // figwheel repl
  "figwheelReplApi"("com.bhauman:figwheel-repl:0.2.18")
  "figwheelReplApi"("ring:ring-jetty-adapter:1.11.0")
  "figwheelReplApi"("org.eclipse.jetty.websocket:websocket-server:9.4.53.v20231009")

  // figwheel main
  "figwheelMainApi"("com.bhauman:figwheel-main:0.2.18")

  // testing
  testRuntimeOnly("org.ajoberstar:jovial:0.4.1")
  devRuntimeOnly("org.slf4j:slf4j-simple:2.0.10")
}

tasks.withType<Test>() {
  useJUnitPlatform()
}

tasks.named<Jar>("jar") {
  dependsOn(configurations.compileClasspath)
  from({
    configurations.compileClasspath.files { dep ->
      dep.getGroup() == "org.gradle"
    }.map {
      zipTree(it).matching { include("org/gradle/**/*") }
    }
  })
}

publishing {
  repositories {
    maven {
      name = "Clojars"
      url = uri("https://repo.clojars.org")
      credentials {
        username = System.getenv("CLOJARS_USER")
        password = System.getenv("CLOJARS_TOKEN")
      }
    }
  }

  publications {
    create<MavenPublication>("main") {
      from(components["java"])

      versionMapping {
        usage("java-api") { fromResolutionOf("runtimeClasspath") }
        usage("java-runtime") { fromResolutionResult() }
      }

      pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/clojurephant/clojurephant-tooling")

        developers {
          developer {
            name.set("Andrew Oberstar")
            email.set("andrew@ajoberstar.org")
          }
        }

        licenses {
          license {
            name.set("Apache License 2.0")
            url.set("https://github.com/clojurephant/jovial/blob/main/LICENSE")
          }
        }

        scm {
          url.set("https://github.com/clojurephant/clojurephant-tooling")
          connection.set("scm:git:git@github.com:clojurephant/clojurephant-tooling.git")
          developerConnection.set("scm:git:git@github.com:clojurephant/clojurephant-tooling.git")
        }
      }
    }
  }
}
