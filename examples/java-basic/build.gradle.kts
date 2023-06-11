plugins {
    java
}

dependencies {
    implementation(project(":core"))
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
