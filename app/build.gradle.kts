import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lilining.lockescape"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lilining.lockescape"
        minSdk = 26
        targetSdk = 34
        versionCode = 30
        versionName = "3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Shizuku 权限通道（Apache-2.0）
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // 协程（逃生动作异步执行）（Apache-2.0）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}

/**
 * 从 releaseRuntimeClasspath 解析结果中提取模块坐标、JAR 内许可文件线索，
 * 生成可核对的传递依赖许可清单，满足 GPL/Apache/MIT 再分发义务的清单化要求。
 */
val generateDependencyLicenseReport by tasks.registering {
    group = "compliance"
    description = "生成 release 运行时依赖的逐项许可清单 docs/DEPENDENCY_LICENSES.md"
    outputs.file(layout.projectDirectory.file("../docs/DEPENDENCY_LICENSES.md"))

    doLast {
        val configuration = configurations.getByName("releaseRuntimeClasspath")
        val resolved = configuration.incoming.resolutionResult
        val modules = linkedSetOf<String>()
        val visited = hashSetOf<Any>()

        fun visit(dependency: ResolvedDependencyResult) {
            val selected = dependency.selected
            if (!visited.add(selected.id)) return
            val id = selected.id
            if (id is ModuleComponentIdentifier) {
                modules += "${id.group}:${id.module}:${id.version}"
            }
            selected.dependencies.forEach { dep ->
                if (dep is ResolvedDependencyResult) visit(dep)
            }
        }

        resolved.root.dependencies.forEach { dep ->
            if (dep is ResolvedDependencyResult) visit(dep)
        }

        val licenseHints = linkedMapOf<String, MutableSet<String>>()
        configuration.incoming.artifacts.artifacts.forEach { artifact ->
            val componentId = artifact.id.componentIdentifier
            if (componentId !is ModuleComponentIdentifier) return@forEach
            val coordinate = "${componentId.group}:${componentId.module}:${componentId.version}"
            val file = artifact.file
            if (file.extension == "jar" || file.extension == "aar") {
                val found = findLicenseMarkers(file)
                if (found.isNotEmpty()) {
                    licenseHints.getOrPut(coordinate) { linkedSetOf() }.addAll(found)
                }
            }
        }

        val report = buildString {
            appendLine("# 传递依赖许可清单（releaseRuntimeClasspath）")
            appendLine()
            appendLine("本清单由 `gradle generateDependencyLicenseReport` 自动生成，")
            appendLine("用于核对分发 APK 时需随附的第三方许可与版权声明。")
            appendLine("坐标按首次解析顺序排列；“JAR 内许可线索”为依赖包内出现的许可文件名或 SPDX 标识，")
            appendLine("不能替代完整许可证正文，完整文本见各上游仓库与根目录 `NOTICE`。")
            appendLine()
            appendLine("| 坐标 | JAR 内许可线索 | 常见许可证（需人工最终确认） |")
            appendLine("|---|---|---|")
            modules.sorted().forEach { coordinate ->
                val hints = licenseHints[coordinate].orEmpty().sorted().joinToString("、").ifBlank { "（包内未检出标准许可文件）" }
                val guess = guessLicense(coordinate)
                appendLine("| `$coordinate` | $hints | $guess |")
            }
            appendLine()
            appendLine("## 直接声明依赖")
            appendLine()
            appendLine("| 依赖 | 许可证 |")
            appendLine("|---|---|")
            appendLine("| androidx.core:core-ktx | Apache-2.0 |")
            appendLine("| androidx.appcompat:appcompat | Apache-2.0 |")
            appendLine("| com.google.android.material:material | Apache-2.0 |")
            appendLine("| androidx.constraintlayout:constraintlayout | Apache-2.0 |")
            appendLine("| dev.rikka.shizuku:api / provider | Apache-2.0 |")
            appendLine("| org.jetbrains.kotlinx:kotlinx-coroutines-android | Apache-2.0 |")
            appendLine("| junit:junit（仅测试） | EPL-1.0 |")
            appendLine()
            appendLine("再分发时应附带各依赖的许可证副本与版权归属；AndroidX / Material / Kotlin / Shizuku 的")
            appendLine("版权归属与 Apache-2.0 全文见根目录 `NOTICE`。")
        }

        val out = layout.projectDirectory.file("../docs/DEPENDENCY_LICENSES.md").asFile
        out.parentFile.mkdirs()
        out.writeText(report)
        println("Wrote ${out.absolutePath} (${modules.size} modules)")
    }
}

fun findLicenseMarkers(file: java.io.File): Set<String> {
    val markers = linkedSetOf<String>()
    try {
        when (file.extension) {
            "jar" -> ZipInputStream(file.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                var count = 0
                while (entry != null && count < 80) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory &&
                        (name.contains("license") || name.contains("notice") ||
                            name.contains("copying") || name.endsWith("meta-inf/manifest.mf"))
                    ) {
                        markers += entry.name.substringAfterLast('/')
                    }
                    if (name.endsWith("pom.properties") || name.endsWith("pom.xml")) {
                        val bytes = zis.readBytes()
                        parseLicenseFromPom(bytes)?.let { markers += it }
                    }
                    entry = zis.nextEntry
                    count++
                }
            }
            "aar" -> ZipInputStream(file.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                var count = 0
                while (entry != null && count < 40) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.contains("license") || name.contains("notice"))) {
                        markers += entry.name.substringAfterLast('/')
                    }
                    entry = zis.nextEntry
                    count++
                }
            }
        }
    } catch (_: Exception) {
        markers += "（读取失败）"
    }
    return markers
}

fun parseLicenseFromPom(bytes: ByteArray): String? {
    return try {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val doc = factory.newDocumentBuilder().parse(bytes.inputStream())
        val nodes = doc.getElementsByTagName("license")
        if (nodes.length == 0) return null
        val name = (nodes.item(0) as org.w3c.dom.Element)
            .getElementsByTagName("name").item(0)?.textContent?.trim()
        name?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

fun guessLicense(coordinate: String): String {
    val g = coordinate.substringBefore(':')
    return when {
        g.startsWith("androidx.") -> "Apache-2.0"
        g.startsWith("com.google.android") -> "Apache-2.0"
        g.startsWith("com.google.errorprone") -> "Apache-2.0"
        g.startsWith("com.google.code.findbugs") -> "Apache-2.0 / LGPL-2.1（jsr305 历史）"
        g.startsWith("dev.rikka.shizuku") -> "Apache-2.0"
        g.startsWith("org.jetbrains.kotlin") -> "Apache-2.0"
        g.startsWith("org.jetbrains.kotlinx") -> "Apache-2.0"
        g.startsWith("org.jetbrains") -> "Apache-2.0"
        g == "junit" -> "EPL-1.0"
        g == "org.hamcrest" -> "BSD-3-Clause / EPL-1.0"
        g == "net.sf.kxml" -> "MIT"
        g == "com.google.guava" -> "Apache-2.0"
        else -> "待人工确认"
    }
}
