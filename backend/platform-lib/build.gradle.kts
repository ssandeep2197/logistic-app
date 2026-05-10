// Aggregator project — has no code of its own. Subprojects under it are the
// real artifacts.  Listed here so `:platform-lib:publishToMavenLocal` walks
// all of them at once.

plugins {
    base
}

tasks.register("publishToMavenLocal") {
    dependsOn(subprojects.map { "${it.path}:publishToMavenLocal" })
}
