val isWindows = System.getProperty("os.name").lowercase().contains("win")

tasks.register<Exec>("buildFrontend") {
    group = "build"
    description = "Build Vue UI (npm install if needed, then npm run build)"
    workingDir = layout.projectDirectory.asFile
    commandLine(
        if (isWindows) {
            listOf(
                "cmd",
                "/c",
                "cd frontend && (if not exist node_modules npm install) && npm run build"
            )
        } else {
            listOf(
                "bash",
                "-c",
                "cd frontend && (test -d node_modules || npm install) && npm run build"
            )
        }
    )
    onlyIf { layout.projectDirectory.file("frontend/package.json").asFile.exists() }
}

tasks.named("build") {
    dependsOn("buildFrontend")
}
