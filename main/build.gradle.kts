/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

plugins {
    id("com.android.application")
    id("checkstyle")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(29)  //'Q'.toInt()
        versionCode = 165
        versionName = "0.7.12"

        externalNativeBuild {
            cmake {
                //arguments = listOf("-DANDROID_TOOLCHAIN=clang",
                //        "-DANDROID_STL=c++_static")
            }
        }
    }

    externalNativeBuild {
        cmake {
            setPath(File("${projectDir}/src/main/cpp/CMakeLists.txt"))
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "build/ovpnassets")
        }

        create("ui") {
            java.srcDirs("src/ovpn3/java/", getOpenvpn3SwigFiles())
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
        }
    }

    lintOptions {
        enable("BackButton", "EasterEgg", "StopShip", "IconExpectedSize", "GradleDynamicVersion", "NewerVersionAvailable")
        warning("ImpliedQuantity", "MissingQuantity")
        disable("MissingTranslation", "UnsafeNativeCodeLocation")
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions("implementation")

    productFlavors {
        create("ui") {
            setDimension("implementation")
            buildConfigField("boolean", "openvpn3", "true")
        }
        create("skeleton") {
            setDimension("implementation")
            buildConfigField("boolean", "openvpn3", "false")
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}

dependencies {
    val preferenceVersion = "1.1.0"
    val coreVersion = "1.1.0"
    val materialVersion = "1.0.0"

    implementation("androidx.annotation:annotation:1.1.0")
    implementation("androidx.core:core:1.1.0")

    // Is there a nicer way to do this?
    dependencies.add("uiImplementation", "androidx.constraintlayout:constraintlayout:1.1.3")
    dependencies.add("uiImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.50")
    dependencies.add("uiImplementation", "androidx.cardview:cardview:1.0.0")
    dependencies.add("uiImplementation", "androidx.recyclerview:recyclerview:1.0.0")
    dependencies.add("uiImplementation", "androidx.appcompat:appcompat:1.1.0")
    dependencies.add("uiImplementation", "com.github.PhilJay:MPAndroidChart:v3.1.0")
    dependencies.add("uiImplementation", "com.squareup.okhttp3:okhttp:3.2.0")
    dependencies.add("uiImplementation", "androidx.core:core:$coreVersion")
    dependencies.add("uiImplementation", "androidx.core:core-ktx:$coreVersion")
    dependencies.add("uiImplementation", "org.jetbrains.anko:anko-commons:0.10.4")
    dependencies.add("uiImplementation", "androidx.fragment:fragment-ktx:1.1.0")
    dependencies.add("uiImplementation", "androidx.preference:preference:$preferenceVersion")
    dependencies.add("uiImplementation", "androidx.preference:preference-ktx:$preferenceVersion")
    dependencies.add("uiImplementation", "com.google.android.material:material:$materialVersion")


    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.50")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:3.1.0")
    testImplementation("org.robolectric:robolectric:4.3.1")
}


/* swig magic for building openvpn3 */
fun getOpenvpn3SwigFiles(): File {
    return File(buildDir, "generated/source/ovpn3swig/ovpn3")
}

tasks.register<Exec>("generateOpenVPN3Swig")
{
    var swigcmd = "swig"
    // Workaround for Mac OS X since it otherwise does not find swig and I cannot get
    // the Exec task to respect the PATH environment :(
    if (File("/usr/local/bin/swig").exists())
        swigcmd = "/usr/local/bin/swig"

    doFirst {
        mkdir(getOpenvpn3SwigFiles())
    }
    commandLine(listOf(swigcmd, "-outdir", getOpenvpn3SwigFiles(), "-outcurrentdir", "-c++", "-java", "-package", "net.openvpn.ovpn3",
            "-Isrc/main/cpp/openvpn3/client", "-Isrc/main/cpp/openvpn3/",
            "-o", "${getOpenvpn3SwigFiles()}/ovpncli_wrap.cxx", "-oh", "${getOpenvpn3SwigFiles()}/ovpncli_wrap.h",
            "src/main/cpp/openvpn3/javacli/ovpncli.i"))
}

/* Hack-o-rama but it works good enough and documentation is surprisingly sparse */

val swigTask = tasks.named("generateOpenVPN3Swig")
val preBuildTask = tasks.getByName("preBuild")
val assembleTask = tasks.getByName("assemble")

assembleTask.dependsOn(swigTask)
preBuildTask.dependsOn(swigTask)


