import com.android.build.gradle.api.LibraryVariant


/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

plugins {
    alias(libVersions.plugins.android.library)
    alias(libVersions.plugins.kotlin.android)
    id("checkstyle")
}

android {
    buildFeatures {
        aidl = true
        buildConfig = true
    }
    namespace = "de.blinkt.openvpn"
    compileSdk = 35
    //compileSdkPreview = "UpsideDownCake"

    // Also update runcoverity.sh
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 21
        targetSdk = 35
        externalNativeBuild {
            cmake {
                //arguments+= "-DCMAKE_VERBOSE_MAKEFILE=1"
            }
        }
    }


    //testOptions.unitTests.isIncludeAndroidResources = true

    externalNativeBuild {
        cmake {
            path = File("${projectDir}/src/main/cpp/CMakeLists.txt")
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "build/ovpnassets")
        }

        create("ui") {
        }
        create("skeleton") {
        }

        getByName("debug") {

        }

        getByName("release") {

        }
    }

    signingConfigs {
        create("release") {
            // ~/.gradle/gradle.properties
            val keystoreFile: String? by project
            storeFile = keystoreFile?.let { file(it) }
            val keystorePassword: String? by project
            storePassword = keystorePassword
            val keystoreAliasPassword: String? by project
            keyPassword = keystoreAliasPassword
            val keystoreAlias: String? by project
            keyAlias = keystoreAlias
            enableV1Signing = true
            enableV2Signing = true
        }

        create("releaseOvpn2") {
            // ~/.gradle/gradle.properties
            val keystoreO2File: String? by project
            storeFile = keystoreO2File?.let { file(it) }
            val keystoreO2Password: String? by project
            storePassword = keystoreO2Password
            val keystoreO2AliasPassword: String? by project
            keyPassword = keystoreO2AliasPassword
            val keystoreO2Alias: String? by project
            keyAlias = keystoreO2Alias
            enableV1Signing = true
            enableV2Signing = true
        }

    }

    lint {
        enable += setOf("BackButton", "EasterEgg", "StopShip", "IconExpectedSize", "GradleDynamicVersion", "NewerVersionAvailable")
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

        create("ovpn23")
        {
            dimension = "ovpnimpl"
            buildConfigField("boolean", "openvpn3", "true")
        }

        create("ovpn2")
        {
            dimension = "ovpnimpl"
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

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    // Ensure native build is run before assets, so assets are ready to be merged into the apk
    libraryVariants.configureEach {
        mergeAssetsProvider.get().dependsOn(externalNativeBuildProviders)
    }
}


var swigcmd = "swig"
// Workaround for macOS(arm64) and macOS(intel) since it otherwise does not find swig and
// I cannot get the Exec task to respect the PATH environment :(
if (file("/opt/homebrew/bin/swig").exists())
    swigcmd = "/opt/homebrew/bin/swig"
else if (file("/usr/local/bin/swig").exists())
    swigcmd = "/usr/local/bin/swig"


fun registerGenTask(variantName: String, variantDirName: String): File {
    val baseDir = File(buildDir, "generated/source/ovpn3swig/${variantDirName}")
    val genDir = File(baseDir, "net/openvpn/ovpn3")

    tasks.register<Exec>("generateOpenVPN3Swig${variantName}")
    {

        doFirst {
            mkdir(genDir)
        }
        commandLine(listOf(swigcmd, "-outdir", genDir, "-outcurrentdir", "-c++", "-java", "-package", "net.openvpn.ovpn3",
                "-Isrc/main/cpp/openvpn3/client", "-Isrc/main/cpp/openvpn3/",
                "-DOPENVPN_PLATFORM_ANDROID",
                "-o", "${genDir}/ovpncli_wrap.cxx", "-oh", "${genDir}/ovpncli_wrap.h",
                "src/main/cpp/openvpn3/client/ovpncli.i"))
        inputs.files( "src/main/cpp/openvpn3/client/ovpncli.i")
        outputs.dir( genDir)

    }
    return baseDir
}

android.libraryVariants.all(object : Action<LibraryVariant> {
    override fun execute(variant: LibraryVariant) {
        val sourceDir = registerGenTask(variant.name, variant.baseName.replace("-", "/"))
        val task = tasks.named("generateOpenVPN3Swig${variant.name}").get()

        variant.registerJavaGeneratingTask(task, sourceDir)
    }
})


dependencies {
    // https://maven.google.com/web/index.html
    implementation(libVersions.androidx.annotation)
    implementation(libVersions.androidx.core.ktx)

    uiImplementation(libVersions.android.view.material)
    uiImplementation(libVersions.androidx.activity)
    uiImplementation(libVersions.androidx.activity.ktx)
    uiImplementation(libVersions.androidx.appcompat)
    uiImplementation(libVersions.androidx.cardview)
    uiImplementation(libVersions.androidx.viewpager2)
    uiImplementation(libVersions.androidx.constraintlayout)
    uiImplementation(libVersions.androidx.core.ktx)
    uiImplementation(libVersions.androidx.fragment.ktx)
    uiImplementation(libVersions.androidx.lifecycle.runtime.ktx)
    uiImplementation(libVersions.androidx.lifecycle.viewmodel.ktx)
    uiImplementation(libVersions.androidx.preference.ktx)
    uiImplementation(libVersions.androidx.recyclerview)
    uiImplementation(libVersions.androidx.security.crypto)
    uiImplementation(libVersions.androidx.webkit)
    uiImplementation(libVersions.kotlin)
    uiImplementation(libVersions.mpandroidchart)
    uiImplementation(libVersions.square.okhttp)

    testImplementation(libVersions.androidx.test.core)
    testImplementation(libVersions.junit)
    testImplementation(libVersions.kotlin)
    testImplementation(libVersions.mockito.core)
    testImplementation(libVersions.robolectric)
}

fun DependencyHandler.uiImplementation(dependencyNotation: Any): Dependency? =
    add("uiImplementation", dependencyNotation)
