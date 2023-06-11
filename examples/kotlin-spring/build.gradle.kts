plugins {
    id("org.springframework.boot") apply true
    id("io.spring.dependency-management") apply true
}

dependencies {
    implementation(project(":interfAIce-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
