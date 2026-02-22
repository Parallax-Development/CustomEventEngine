plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.mvel:mvel2:2.5.2.Final")

    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
group = "com.darkbladedev.cee"
version = "1.0.10"

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("CustomEventEngine")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}
