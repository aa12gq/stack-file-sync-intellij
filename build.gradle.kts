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
version = "1.0.1"

repositories {
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3.4")
    type.set("IC")
    plugins.set(listOf(
        "Git4Idea",
        "java",
        "platform-images"  // 移除版本号
    ))
    updateSinceUntilBuild.set(false)
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
        sinceBuild.set("233")
        untilBuild.set("241.*")
        // 添加插件描述
        pluginDescription.set("""
            A powerful plugin for synchronizing files and code snippets between multiple projects.
            
            Features:
            - Real-time file synchronization
            - Configurable auto-sync intervals
            - Multi-repository support
            - Secure file backup
            - High-performance file transfer
            - Customizable sync rules
            - Detailed sync logs
            
            For more information, visit: https://github.com/aa12gq/stack-file-sync-intellij
        """.trimIndent())
        // 添加更新说明
        changeNotes.set("""
            <ul>
                <li>1.0.1
                    <ul>
                        <li>Added import/export configuration feature</li>
                        <li>Fixed auto-sync issues</li>
                        <li>Improved UI experience</li>
                    </ul>
                </li>
            </ul>
        """.trimIndent())
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
