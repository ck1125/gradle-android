package org.gradle.android.internal

import org.gradle.android.BuildType
import org.gradle.api.tasks.SourceSet

class BuildTypeDimension {
    final BuildType buildType
    final Set<AndroidAppVariant> variants = []
    final SourceSet mainSource

    BuildTypeDimension(BuildType buildType, SourceSet mainSource) {
        this.buildType = buildType
        this.mainSource = mainSource
    }

    String getName() {
        return buildType.name
    }

    String getAssembleTaskName() {
        return "assemble${buildType.name.capitalize()}"
    }
}
