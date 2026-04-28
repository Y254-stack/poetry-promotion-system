pluginManagement {
    repositories {
        // 1. 阿里云的 Gradle 插件镜像 (用于替换 gradlePluginPortal())
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        // 2. 阿里云的 Google 插件镜像 (用于替换 google())
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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
        // 1. 阿里云的 Google 依赖镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        // 2. 阿里云的公共依赖镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }

        google()
        mavenCentral()
    }
}

rootProject.name = "Poetry"
include(":app")
include(":backend")
 