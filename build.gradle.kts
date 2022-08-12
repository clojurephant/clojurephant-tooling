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
  compileOnly("org.gradle:gradle-tooling-api:7.5")

  // clojurescript and nrepl
  api("org.clojure:clojurescript:1.11.60")
  api("nrepl:nrepl:0.9.0")
  api("cider:piggieback:0.5.3")

  // figwheel repl
  "figwheelReplApi"("com.bhauman:figwheel-repl:0.2.18")
  "figwheelReplApi"("ring:ring-jetty-adapter:1.9.5")
  "figwheelReplApi"("org.eclipse.jetty.websocket:websocket-server:9.4.7.v20180619")

  // figwheel main
  "figwheelMainApi"("com.bhauman:figwheel-main:0.2.18")

  // testing
  testRuntimeOnly("org.ajoberstar:jovial:0.3.0")
  devRuntimeOnly("org.slf4j:slf4j-simple:1.7.36")
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
    }
  }
}

tasks.withType<GenerateModuleMetadata>() {
  enabled = false
}
