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
        }
    }
}

dependencies {
    // Open Parties and Claims 0.33.0 per a NeoForge 1.21.1 (id de versió de Modrinth, el mateix que a mods-lock.json).
    // Només per compilar: al servidor ja hi és.
    compileOnly("maven.modrinth:open-parties-and-claims:yWmXD7GW")

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
