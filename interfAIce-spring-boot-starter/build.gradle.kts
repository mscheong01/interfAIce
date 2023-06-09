
dependencies {
    api(project(":core"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
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
                name.set("interfAIce spring boot starter")
                artifactId = "interfAIce-spring-boot-starter"
                description.set("interfAIce spring boot starter")
            }
        }
    }
}
