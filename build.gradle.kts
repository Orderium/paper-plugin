plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.gradleup.shadow") version "9.4.3"
    id ("com.modrinth.minotaur") version "2.+"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

val pluginVersion = "2.8.0"
val releaseType = "release"
val mcVersions = listOf(
    "1.21.8", "1.21.9", "1.21.10", "1.21.11",
    "26.1", "26.1.1", "26.1.2", "26.2", "26.3"
)

project.version = pluginVersion

repositories {
    mavenCentral()
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "paper-repo"
    }
    maven(url = "https://repo.bsdevelopment.org/releases/") {
        name = "cm-repo"
    }

    maven(url = "https://repo.faststats.dev/releases") {
        name = "faststats-repo"
    }

    maven(url = "https://jitpack.io")

    maven(url = "https://repo.codemc.io/repository/maven-releases/")
    maven(url = "https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
//    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    implementation("dev.faststats.metrics:bukkit:0.32.2")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.3")
    implementation("com.zaxxer:HikariCP:7.0.2")
}

paperweight {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    runServer {
        minecraftVersion("26.3")
        jvmArgs("-Dfaststats.debug=true", "-Dcom.mojang.eula.agree=true")
    }

    shadowJar {
        archiveClassifier = ""
        relocate("com.zaxxer", "orderium.zaxxer")
        relocate("org.bstats", "orderium.bstats")
        relocate("dev.faststats", "orderium.faststats")
    }
}

val targetJavaVersion = 25

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

val faststatsToken = providers.environmentVariable("FASTSTATS_TOKEN")
    .orElse(providers.gradleProperty("faststats.token"))
    .orElse(providers.fileContents(layout.projectDirectory.file("faststats.token")).asText.map { it.trim() })
    .orElse("")

tasks.processResources {
    inputs.property("version", pluginVersion)
    inputs.property("faststatsToken", faststatsToken)

    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(inputs.properties)
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId = "orderium"
    versionNumber = pluginVersion
    versionType = releaseType
    uploadFile.set(tasks.shadowJar)
    gameVersions = mcVersions
    loaders = listOf("paper", "purpur", "folia")
    changelog = rootProject.file("modrinth/CHANGELOG.md").readText()
}