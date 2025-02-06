plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.16.1"
}

dependencies {
    implementation("com.intellij:forms_rt:7.0.3")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
}

group = "com.stackfilesync"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1") // 指定 IDE 版本，2023.1 是 IntelliJ IDEA 2023.1 版本
    type.set("IC") // IC 表示 IntelliJ IDEA Community Edition
    plugins.set(listOf(
        "Git4Idea",  // 注意大小写
        "java",       //  Java 插件依赖
        "platform-images"  // 添加图标支持
    ))
    updateSinceUntilBuild.set(false)  // 添加这行，避免版本兼容性问题
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set("231") // 支持的最低 IDE 版本
        untilBuild.set("233.*") // 支持的最高 IDE 版本
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN")) // JetBrains Marketplace 的发布令牌
    }

    // 禁用搜索选项构建任务
    buildSearchableOptions {
        enabled = false
    }
}
