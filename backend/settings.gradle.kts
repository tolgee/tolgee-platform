rootProject.name = "tolgee-backend"

include(":common")
include(":data")
include(":security")
include(":api")
include(":app")

if (file("ee").exists()) {
    include(":ee-app")
    project(":ee-app").projectDir = file("ee/backend/app")
} 