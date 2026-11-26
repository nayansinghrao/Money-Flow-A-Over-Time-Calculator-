// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Keep a phone-installable copy of the latest successful debug build.
val exportMoneyFlowApk = tasks.register<Copy>("exportMoneyFlowApk") {
    dependsOn(":app:packageDebug")
    from(layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk"))
    into(layout.projectDirectory.dir("APK"))
    rename { "MoneyFlow.apk" }
}

project(":app") {
    tasks.matching { it.name == "assembleDebug" }.configureEach {
        dependsOn(exportMoneyFlowApk)
    }
}
