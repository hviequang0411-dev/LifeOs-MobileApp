import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Khóa ký bản release đọc từ android/key.properties (đã nằm trong .gitignore, KHÔNG commit).
// Thiếu file này thì bản release không build được — không bao giờ ký release bằng khóa debug (P-DWP7-03).
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    // namespace là package mã nguồn Kotlin (MainActivity), giữ nguyên; định danh cài đặt là applicationId bên dưới.
    namespace = "com.lifeos.lifeos_mobile"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // P-DWP7-01 [CONFIRMED]: định danh production thống nhất vn.lifeos.app.
        applicationId = "vn.lifeos.app"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        multiDexEnabled = true
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = keystoreProperties.getProperty("storeFile")?.let { file(it) }
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

// Chặn build bản release khi chưa có keystore riêng (chỉ kiểm lúc thực sự đóng gói release, debug không bị ảnh hưởng).
gradle.taskGraph.whenReady {
    val packagingRelease = allTasks.any { task ->
        (task.name.startsWith("assemble") || task.name.startsWith("bundle")) && task.name.endsWith("Release")
    }
    if (packagingRelease && !keystorePropertiesFile.exists()) {
        throw GradleException(
            "Thiếu android/key.properties: bản release phải ký bằng keystore riêng. " +
                "Xem docs/ops/SELF_HOST_TUNNEL.md mục 2 (tạo keystore)."
        )
    }
}
