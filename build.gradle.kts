plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.jandie1505"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.github.ProxioDev.ValioBungee:RedisBungee-Velocity:0.12.3")
}

tasks {
    shadowJar {
        //relocate("net.chaossquad.mclib", "net.jandie1505.bedwars.dependencies.net.chaossquad.mclib")
        //relocate("org.json", "net.jandie1505.bedwars.dependencies.org.json")
    }
    build {
        dependsOn(shadowJar)
    }
}
