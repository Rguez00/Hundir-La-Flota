import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeMaterial3)
            implementation(libs.composeUi)
            implementation(libs.composeResources)
            implementation(libs.composeUiToolingPreview)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.6.0") // ✅ CORREGIDO

            implementation(libs.lifecycleViewmodelCompose)
            implementation(libs.lifecycleRuntimeCompose)

            implementation(libs.kotlinxSerializationJsonLib)
        }

        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.junit)
        }

        commonTest.dependencies {
            implementation(libs.kotlinTest)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinxCoroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mario.hlf.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "com.mario.hlf"
            packageVersion = "1.0.0"
        }
    }
}