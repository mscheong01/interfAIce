
dependencies {
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.1")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-reactor
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.1")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-reactive
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.1")
    // https://mvnrepository.com/artifact/com.squareup.okhttp/okhttp
    implementation("com.squareup.okhttp:okhttp:2.7.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    // https://mvnrepository.com/artifact/org.assertj/assertj-core
    testImplementation("org.assertj:assertj-core:3.24.2")

}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("interfAIce core library")
                artifactId = "interfAIce-core"
                description.set("interfAIce core library")
            }
        }
    }
}
