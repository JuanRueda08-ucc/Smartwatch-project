// AGP 9 trae Kotlin integrado. Declarar kotlin-android aquí fija la versión de Kotlin (2.4.21)
// para que coincida con el plugin del compilador de Compose (ver DECISIONES.md, D-014).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
