import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskProvider

/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

plugins {
    alias(libs.plugins.android.application)
    id("checkstyle")
}

/**
 * Pick the preferred NDK if it is installed, otherwise fall back to a known available version.
 * This lets the build succeed on machines that do not have the exact upstream NDK revision.
 */
fun resolveNdkVersion(preferred: String, fallback: String): String {
    val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: return preferred
    return if (file("$sdkRoot/ndk/$preferred").exists()) preferred else fallback
}


fun obtainTestBuildType(): String {
    var result = "debug";

    if (project.hasProperty("testBuildType")) {
        result = project.property("testBuildType").toString()
    }
    return result
}

/**
 * Resolve a signing configuration value.
 * First checks Gradle project properties (e.g. ~/.gradle/gradle.properties),
 * then falls back to environment variables so CI secrets can be used.
 * Property names are converted to UPPER_SNAKE_CASE for env lookups,
 * e.g. "keystoreFile" -> "KEYSTORE_FILE".
 */
fun signingProperty(name: String): String? {
    val projectValue = project.findProperty(name) as? String
    if (projectValue != null) return projectValue

    val envName = name.replace(Regex("([A-Z])"), "_$1").uppercase()
    return System.getenv(envName)
}

android {
    buildFeatures {
        aidl = true
        buildConfig = true
    }
    namespace = "de.blinkt.openvpn"
    compileSdk = 37
    //compileSdkPreview = "UpsideDownCake"

    // Also update runcoverity.sh
    ndkVersion = resolveNdkVersion("30.0.14904198", "29.0.14206865")

    defaultConfig {
        minSdk = 23
        targetSdk = 37
        //targetSdkPreview = "UpsideDownCake"
        versionCode = 219
        versionName = "0.7.64"
        externalNativeBuild {
            cmake {
                //arguments+= "-DCMAKE_VERBOSE_MAKEFILE=1"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = File("${projectDir}/src/main/cpp/CMakeLists.txt")
        }
    }

    sourceSets {
        getByName("main") {
            assets.directories.add("build/ovpnassets")
        }

        create("ui") {}

        create("skeleton") {}

        create("sargo") {
            java.setSrcDirs(listOf("src/ui/java", "src/sargo/java"))
            kotlin.setSrcDirs(listOf("src/ui/java", "src/sargo/java"))
            res.setSrcDirs(listOf("src/ui/res", "src/sargo/res"))
            manifest.srcFile("src/sargo/AndroidManifest.xml")
        }

        getByName("debug") {}

        getByName("release") {}
    }

    signingConfigs {
        create("release") {
            // Values may come from ~/.gradle/gradle.properties or CI environment variables.
            val keystoreFile = signingProperty("keystoreFile")
            storeFile = keystoreFile?.let { file(it) }
            storePassword = signingProperty("keystorePassword")
            keyPassword = signingProperty("keystoreAliasPassword")
            keyAlias = signingProperty("keystoreAlias")
            enableV1Signing = true
            enableV2Signing = true
        }

        create("releaseOvpn2") {
            // Values may come from ~/.gradle/gradle.properties or CI environment variables.
            // Fall back to the main release keystore if ovpn2-specific values are not set.
            val keystoreO2File = signingProperty("keystoreO2File") ?: signingProperty("keystoreFile")
            storeFile = keystoreO2File?.let { file(it) }
            storePassword = signingProperty("keystoreO2Password") ?: signingProperty("keystorePassword")
            keyPassword = signingProperty("keystoreO2AliasPassword") ?: signingProperty("keystoreAliasPassword")
            keyAlias = signingProperty("keystoreO2Alias") ?: signingProperty("keystoreAlias")
            enableV1Signing = true
            enableV2Signing = true
        }

    }

    lint {
        enable += setOf(
            "BackButton",
            "EasterEgg",
            "StopShip",
            "IconExpectedSize",
            "GradleDynamicVersion",
            "NewerVersionAvailable"
        )
        checkOnly += setOf("ImpliedQuantity", "MissingQuantity")
        disable += setOf("MissingTranslation", "UnsafeNativeCodeLocation")
    }


    flavorDimensions += listOf("implementation", "ovpnimpl")

    productFlavors {
        create("ui") {
            dimension = "implementation"
        }

        create("skeleton") {
            dimension = "implementation"
        }

        create("sargo") {
            dimension = "implementation"
            applicationId = "de.blinkt.openvpn"
            versionNameSuffix = "-sargo"
        }

        create("ovpn23") {
            dimension = "ovpnimpl"
            buildConfigField("boolean", "openvpn3", "true")
        }

        create("ovpn2") {
            dimension = "ovpnimpl"
            versionNameSuffix = "-o2"
            buildConfigField("boolean", "openvpn3", "false")
        }
    }

    buildTypes {
        getByName("release") {
            if (project.hasProperty("icsopenvpnDebugSign")) {
                logger.warn("property icsopenvpnDebugSign set, using debug signing for release")
                signingConfig = android.signingConfigs.getByName("debug")
            } else {
                productFlavors["ovpn23"].signingConfig = signingConfigs.getByName("release")
                productFlavors["ovpn2"].signingConfig = signingConfigs.getByName("releaseOvpn2")
            }
        }
    }

    testBuildType = obtainTestBuildType()

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    bundle {
        codeTransparency {
            signing {
                val keystoreTPFile: String? by project
                storeFile = keystoreTPFile?.let { file(it) }
                val keystoreTPPassword: String? by project
                storePassword = keystoreTPPassword
                val keystoreTPAliasPassword: String? by project
                keyPassword = keystoreTPAliasPassword
                val keystoreTPAlias: String? by project
                keyAlias = keystoreTPAlias

                if (keystoreTPFile?.isEmpty()
                        ?: true
                ) println("keystoreTPFile not set, disabling transparency signing")
                if (keystoreTPPassword?.isEmpty()
                        ?: true
                ) println("keystoreTPPassword not set, disabling transparency signing")
                if (keystoreTPAliasPassword?.isEmpty()
                        ?: true
                ) println("keystoreTPAliasPassword not set, disabling transparency signing")
                if (keystoreTPAlias?.isEmpty()
                        ?: true
                ) println("keyAlias not set, disabling transparency signing")

            }
        }
    }
}

var swigcmd = "swig"
// Workaround for macOS(arm64) and macOS(intel) since it otherwise does not find swig and
// I cannot get the Exec task to respect the PATH environment :(
if (file("/opt/homebrew/bin/swig").exists()) swigcmd = "/opt/homebrew/bin/swig"
else if (file("/usr/local/bin/swig").exists()) swigcmd = "/usr/local/bin/swig"


abstract class GenerateSwigTask : Exec() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}

fun registerGenSwigTask(variantName: String, variantDirName: String): TaskProvider<GenerateSwigTask> {
    val baseDir = layout.buildDirectory.dir("generated/source/ovpn3swig/${variantDirName}")

    val genTask = tasks.register<GenerateSwigTask>("generateOpenVPN3Swig${variantName}") {
        val genDir = baseDir.get().asFile.resolve("net/openvpn/ovpn3")
        outputDir.set(baseDir)

        doFirst {
            mkdir(genDir)
        }
        commandLine(
            listOf(
                swigcmd,
                "-outdir",
                genDir.absolutePath,
                "-outcurrentdir",
                "-c++",
                "-java",
                "-package",
                "net.openvpn.ovpn3",
                "-Isrc/main/cpp/openvpn3/client",
                "-Isrc/main/cpp/openvpn3/",
                "-DOPENVPN_PLATFORM_ANDROID",
                "-o",
                "${genDir}/ovpncli_wrap.cxx",
                "-oh",
                "${genDir}/ovpncli_wrap.h",
                "src/main/cpp/openvpn3/client/ovpncli.i"
            )
        )
        inputs.files("src/main/cpp/openvpn3/client/ovpncli.i")

    }
    return genTask
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val execTask = registerGenSwigTask(variant.name, variant.name.replace("-", "/"))
        variant.sources.java?.addGeneratedSourceDirectory(execTask, GenerateSwigTask::outputDir)
    }
}

dependencies {
    // https://maven.google.com/web/index.html
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)

    uiImplementation(libs.android.view.material)
    uiImplementation(libs.androidx.activity)
    uiImplementation(libs.androidx.activity.ktx)
    uiImplementation(libs.androidx.appcompat)
    uiImplementation(libs.androidx.cardview)
    uiImplementation(libs.androidx.viewpager2)
    uiImplementation(libs.androidx.constraintlayout)
    uiImplementation(libs.androidx.core.ktx)
    uiImplementation(libs.androidx.fragment.ktx)
    uiImplementation(libs.androidx.lifecycle.runtime.ktx)
    uiImplementation(libs.androidx.lifecycle.viewmodel.ktx)
    uiImplementation(libs.androidx.preference.ktx)
    uiImplementation(libs.androidx.recyclerview)
    uiImplementation(libs.androidx.security.crypto)
    uiImplementation(libs.kotlin)
    uiImplementation(libs.mpandroidchart)
    uiImplementation(libs.square.okhttp)

    // SargO MDM client library (release AAR built from SargO/sargo/launcher/lib).
    sargoImplementation(files("src/sargo/libs/sargo-mdm-lib-release.aar"))

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
}

fun DependencyHandler.uiImplementation(dependencyNotation: Any): Dependency? {
    add("uiImplementation", dependencyNotation)
    return add("sargoImplementation", dependencyNotation)
}

fun DependencyHandler.sargoImplementation(dependencyNotation: Any): Dependency? =
    add("sargoImplementation", dependencyNotation)
