dependencies {
}

// disable publishing
tasks.withType<AbstractPublishToMaven> {
    enabled = false
}
subprojects {
    tasks.withType<AbstractPublishToMaven> {
        enabled = false
    }
}
