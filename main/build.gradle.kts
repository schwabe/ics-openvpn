/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
import java.util.Properties

plugins {
    id("com.android.application")
    id("checkstyle")
}

apply {
    plugin("kotlin-android")
    plugin("kotlin-android-extensions")
}

repositories {
    jcenter()
    maven(url = "https://jitpack.io")
    google()
}


val openvpn3SwigFiles = File(buildDir, "generated/source/ovpn3swig/ovpn3")

tasks.register<Exec>("generateOpenVPN3Swig")
{
    var swigcmd = "swig"
    // Workaround for Mac OS X since it otherwise does not find swig and I cannot get
    // the Exec task to respect the PATH environment :(
    if (File("/usr/local/bin/swig").exists())
        swigcmd = "/usr/local/bin/swig"

    doFirst {
        mkdir(openvpn3SwigFiles)
    }
    commandLine(listOf(swigcmd, "-outdir", openvpn3SwigFiles, "-outcurrentdir", "-c++", "-java", "-package", "net.openvpn.ovpn3",
            "-Isrc/main/cpp/openvpn3/client", "-Isrc/main/cpp/openvpn3/",
            "-o", "${openvpn3SwigFiles}/ovpncli_wrap.cxx", "-oh", "${openvpn3SwigFiles}/ovpncli_wrap.h",
            "src/main/cpp/openvpn3/javacli/ovpncli.i"))
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(28)  //'Q'.toInt()
        versionCode = 161
        versionName = "0.7.8"

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
            java.srcDirs("src/ovpn3/java/", openvpn3SwigFiles)
        }
        create("skeleton") {
        }

        getByName("debug") {

        }

        getByName("release") {

        }
    }

    signingConfigs {
        create("release") {}
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
            setEnable(true)
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            setUniversalApk(true)

        }
    }
}

// ~/.gradle/gradle.properties
if (project.hasProperty("keystoreFile") &&
        project.hasProperty("keystorePassword") &&
        project.hasProperty("keystoreAliasPassword")) {
    android.signingConfigs.getByName("release") {
        storeFile = file(project.properties["keystoreFile"] as String)
        storePassword = project.properties["keystorePassword"] as String
        keyPassword = project.properties["keystoreAliasPassword"] as String
        keyAlias = project.properties["keystoreAlias"] as String
    }
} else {
    android.buildTypes.getByName("release").signingConfig = null
}


/* Hack-o-rama but it works good enough and documentation is surprisingly sparse */

val swigTask = tasks.named("generateOpenVPN3Swig")
val preBuildTask = tasks.getByName("preBuild")
val assembleTask = tasks.getByName("assemble")

assembleTask.dependsOn(swigTask)
preBuildTask.dependsOn(swigTask)

/* Normally you would put these on top but then it errors out on unknown configurations */
dependencies {
    implementation("com.android.support:support-annotations:28.0.0")
    implementation("com.android.support:support-core-utils:28.0.0")

    // Is there a nicer way to do this?
    dependencies.add("uiImplementation", "com.android.support.constraint:constraint-layout:1.1.3")
    dependencies.add("uiImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.40")
    dependencies.add("uiImplementation","com.android.support.constraint:constraint-layout:1.1.3")
    dependencies.add("uiImplementation","com.android.support:cardview-v7:28.0.0")
    dependencies.add("uiImplementation","com.android.support:recyclerview-v7:28.0.0")
    dependencies.add("uiImplementation","com.github.PhilJay:MPAndroidChart:v3.0.2")


    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("org.robolectric:robolectric:4.3")
}

