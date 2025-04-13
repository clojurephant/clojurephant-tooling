plugins {
  id("dev.clojurephant.clojure")
  id("java-library")
  id("maven-publish")
}

group = "dev.clojurephant"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
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
  api("org.clojure:clojure:1.12.0")

  // gradle
  compileOnly("org.gradle:gradle-tooling-api:8.13")

  // clojurescript and nrepl
  api("org.clojure:clojurescript:1.11.132")
  api("nrepl:nrepl:1.3.1")
  api("cider:piggieback:0.6.0")

  // figwheel repl
  "figwheelReplApi"("com.bhauman:figwheel-repl:0.2.20")
  "figwheelReplApi"("ring:ring-jetty-adapter:1.13.0")
  "figwheelReplApi"("org.eclipse.jetty.websocket:websocket-server:9.4.57.v20241219")

  // figwheel main
  "figwheelMainApi"("com.bhauman:figwheel-main:0.2.20")

  // testing
  testRuntimeOnly("dev.clojurephant:jovial:0.4.2")
  devRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

tasks.withType<Test>() {
  useJUnitPlatform()
}

tasks.named<Jar>("jar") {
  from(configurations.compileClasspath.get().map {
    if (it.isDirectory()) {
      it
    } else {
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
            name.set("Apache-2.0")
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
