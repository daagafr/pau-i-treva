// Pau i Treva: mod de països per a MineUAB Create (NeoForge 1.21.1).
// Compila amb `./gradlew build` (a GitHub Actions, vegeu .github/workflows/compila.yml).
plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.148"
}

val versioMod = "0.1.0"
version = versioMod
group = "cat.mineuab.pauitreva"

base {
    archivesName = "pauitreva"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

// Proves dins d'un servidor de veritat (GameTests de NeoForge) amb OPAC: `./gradlew runProves`.
// No entren al jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[gametest.compileOnlyConfigurationName].extendsFrom(configurations.compileOnly.get())

repositories {
    // Mods de Modrinth (Open Parties and Claims). Només aquest grup, perquè cap altra dependència en surti.
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") { name = "Modrinth" }
        }
        filter { includeGroup("maven.modrinth") }
    }
}

neoForge {
    // La mateixa versió que el servidor del Create.
    version = "21.1.264"

    mods {
        create("pauitreva") {
            sourceSet(sourceSets.main.get())
            sourceSet(gametest)
        }
    }

    addModdingDependenciesTo(gametest)

    runs {
        // Un servidor dedicat de proves que fa les GameTests en arrencar i s'atura (vegeu ExecutorProves).
        // El de GameTest de Minecraft no serveix: OPAC hi peta en arrencar.
        create("proves") {
            server()
            sourceSet = gametest
            gameDirectory = project.file("build/proves-servidor")
            programArgument("--nogui")
            systemProperty("pauitreva.proves", "true")
            systemProperty("neoforge.enableGameTest", "true")
            systemProperty("neoforge.enabledGameTestNamespaces", "pauitreva")
        }
    }
}

dependencies {
    // Open Parties and Claims 0.33.0 per a NeoForge 1.21.1 (id de versió de Modrinth, el mateix que a mods-lock.json).
    // Només per compilar: al servidor ja hi és.
    compileOnly("maven.modrinth:open-parties-and-claims:yWmXD7GW")
    // Per a les GameTests, OPAC ha de ser-hi de veritat.
    "gametestRuntimeOnly"("maven.modrinth:open-parties-and-claims:yWmXD7GW")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val propietats = mapOf("versio" to versioMod)
    inputs.properties(propietats)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(propietats)
    }
}

// El servidor de proves comença cada cop de zero, amb un món pla i sense comprovar comptes.
tasks.matching { it.name == "runProves" }.configureEach {
    doFirst {
        val dir = project.file("build/proves-servidor")
        dir.deleteRecursively()
        dir.mkdirs()
        dir.resolve("eula.txt").writeText("eula=true\n")
        dir.resolve("server.properties").writeText(
            "online-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\nview-distance=4\n" +
                "simulation-distance=4\ngenerate-structures=false\nserver-port=25599\n"
        )
        // La mateixa config per defecte d'OPAC que el servidor del Create (sense PvP ni explosions als claims).
        val opac = project.file("../servidor/world/serverconfig/openpartiesandclaims-default-player-config.toml")
        if (opac.exists()) {
            opac.copyTo(dir.resolve("world/serverconfig/openpartiesandclaims-default-player-config.toml"))
        }
    }
}
