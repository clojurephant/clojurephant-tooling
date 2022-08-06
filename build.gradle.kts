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
}

dependencies {
  implementation("org.clojure:clojure:1.11.1")
  implementation("org.gradle:gradle-tooling-api:7.5")
  testRuntimeOnly("org.ajoberstar:jovial:0.3.0")
  devRuntimeOnly("org.slf4j:slf4j-simple:1.7.36")
}

tasks.withType<Test>() {
  useJUnitPlatform()
}

publishing {
  repositories {
    maven {
      name = "Clojars"
      url = uri("https://repo.clojars.org")
      credentials {
        username = System.getenv("CLOJARS_USER")
        password = System.getenv("CLOJARS_PASSWORD")
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
