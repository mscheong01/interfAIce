rootProject.name = "interfAIce"
include("core")
include("interfAIce-spring-boot-starter")
include("examples")
include("examples:kotlin-basic")
include("examples:java-basic")
findProject(":examples:java-basic")?.name = "java-basic"
