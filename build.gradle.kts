plugins {
    java
    application
}

apply(from = "gradle/config.gradle.kts")

val javaVersion = extra["javaVersion"] as Int
val mainClassName = extra["mainClass"] as String
val sqliteVersion = extra["sqliteVersion"] as String

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src/server"))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("org.jsoup:jsoup:1.18.3")
}

application {
    mainClass.set(mainClassName)
    applicationDefaultJvmArgs = listOf(
        "-Dserver.browser.root=${layout.projectDirectory.asFile.absolutePath}"
    )
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.named<JavaExec>("run") {
    workingDir = layout.projectDirectory.asFile
    dependsOn("buildFrontend")
}

apply(from = "gradle/frontend.gradle.kts")

tasks.register<Jar>("serverJar") {
    group = "build"
    description = "Fat jar with dependencies"
    archiveBaseName.set("server-browser")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    manifest {
        attributes["Main-Class"] = mainClassName
    }
}
