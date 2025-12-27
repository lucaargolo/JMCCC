plugins {
    base
}

group = "dev.3-3"
version = project.properties["version"].toString()

subprojects {
    //Real subproject DSL is located at `buildSrc/src/main/kotlin/dev.3-3.jmccc.gradle.kts`
    apply(plugin = "dev.3-3.jmccc")
}