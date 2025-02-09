plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.2"
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.google.code.gson:gson:2.10.1")
}

group = "com.stackfilesync"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3.4") // 指定具体版本
    type.set("IC") // IC 表示 IntelliJ IDEA Community Edition
    plugins.set(listOf(
        "Git4Idea",  // 注意大小写
        "java",      // Java 插件依赖
        "platform-images"  // 添加图标支持
    ))
    updateSinceUntilBuild.set(false)  // 禁用版本限制
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
        sinceBuild.set("233")  // 更新为当前IntelliJ版本
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // 禁用搜索选项构建任务
    buildSearchableOptions {
        enabled = false
    }
    
    // 配置打包任务，排除 IDE 包
    prepareSandbox {
        exclude("com/intellij/uiDesigner/**")
    }
}

// 配置 jar 任务，排除 IDE 包
tasks.jar {
    exclude("com/intellij/uiDesigner/**")
    exclude("com/intellij/uiDesigner/core/**")
    exclude("com/intellij/uiDesigner/lw/**")
    exclude("com/intellij/uiDesigner/compiler/**")
    exclude("com/intellij/uiDesigner/shared/**")
}
