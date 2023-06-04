plugins {
    kotlin("jvm") version "1.8.20" apply false
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    signing
    id("org.springframework.boot") version "3.0.5" apply false
    id("io.spring.dependency-management") version "1.1.0" apply false
}

group = "io.github.mscheong01"
if (project.hasProperty("releaseVersion")) {
    version = project.property("releaseVersion") as String
}

ext["coroutinesVersion"] = "1.6.2"

repositories {
    mavenCentral()
}

subprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("maven-publish")
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("signing")
    }

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = JavaVersion.VERSION_17.toString()
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                pom {
                    url.set("https://github.com/mscheong01/interfAIce")
                    licenses {
                        license {
                            name.set("Apache 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("mscheong01")
                            name.set("Minsoo Cheong")
                            email.set("icycle0409@snu.ac.kr")
                            organization.set("MinsooCheong")
                            organizationUrl.set("https://github.com/mscheong01")
                        }
                    }
                    scm {
                        connection.set("scm:git:git@github.com:mscheong01/interfAIce.git")
                        developerConnection.set("scm:git:git@github.com:mscheong01/interfAIce.git")
                        url.set("https://github.com/mscheong01/interfAIce")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "OSSRH"
                url = if ((version as String).endsWith("-SNAPSHOT")) {
                    uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                } else {
                    uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                }
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        ktlint {
            filter {
                exclude { entry ->
                    entry.file.toString().contains("generated")
                }
            }
        }
    }

    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }

    tasks.withType<Sign>().configureEach {
        onlyIf {
            project.hasProperty("releaseVersion")
        }
    }
}

project(":spring-boot-starter-interfAIce") {
    apply {
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
    }
}

tasks.create("updateVersion") {
    if (rootProject.hasProperty("next")) {
        val file = File(rootProject.rootDir, "gradle.properties")
        val prop = java.util.Properties().apply { load(java.io.FileInputStream(file)) }
        val ver = rootProject.property("next") as String
        if (ver != prop.getProperty("version")) {
            prop.setProperty("version", ver)
            prop.store(java.io.FileOutputStream(file), null)
        }
    }
}
