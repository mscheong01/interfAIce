
dependencies {
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    api("com.fasterxml.jackson.core:jackson-databind:2.15.1")
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.1")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.ext["coroutinesVersion"]}")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-reactor
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${rootProject.ext["coroutinesVersion"]}}")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-reactive
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${rootProject.ext["coroutinesVersion"]}")
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
            pom {
                name.set("interfAIce core library")
                artifactId = "interfAIce-core"
                description.set("interfAIce core library")
            }
        }
    }
}
