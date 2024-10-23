pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mediquo.jfrog.io/artifactory/android-sdk") }
        maven { url = uri("https://mediquo.jfrog.io/artifactory/videocall-android") }
    }
}

rootProject.name = "MediquoSdkTest"
include(":app")
