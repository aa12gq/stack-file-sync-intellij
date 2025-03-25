plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.jetbrains.intellij") version "1.13.3"
}

// JVM 参数以支持 TLS
allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Dhttps.protocols=TLSv1.2,TLSv1.3")
    }
}

// 配置 Gradle JVM 参数
tasks.withType<Test> {
    systemProperty("https.protocols", "TLSv1.2,TLSv1.3")
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.google.code.gson:gson:2.10.1")
}

group = "com.stackfilesync"
version = "1.3.1"

repositories {
    mavenCentral()
    // 阿里云镜像源作为备选
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
    maven {
        url = uri("https://maven.aliyun.com/repository/gradle-plugin")
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1")
    type.set("IC")
    plugins.set(listOf("git4idea"))
    updateSinceUntilBuild.set(true)
    sinceBuild.set("231")
    untilBuild.set("233.*")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            languageVersion = "1.8"
            apiVersion = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set("231")
        untilBuild.set("233.*")
        
        changeNotes.set("""
            <h3>1.2.0</h3>
            <ul>
                <li>Added P2P file transfer functionality</li>
                <li>Added P2P node configuration</li>
                <li>Support custom file receive directory</li>
                <li>Added file transfer progress display</li>
                <li>Support batch file transfer</li>
            </ul>
            
            <h3>1.0.1</h3>
            <ul>
                <li>Fixed known issues</li>
                <li>Improved user interface</li>
            </ul>
            
            <h3>1.0.0</h3>
            <ul>
                <li>Initial release</li>
                <li>Support file synchronization</li>
                <li>Support auto sync</li>
                <li>Support sync history</li>
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

kotlin {
    jvmToolchain(17)
    
    sourceSets.all {
        languageSettings {
            languageVersion = "1.8"
            apiVersion = "1.8"
        }
    }
}
