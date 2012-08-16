package org.gradle.android

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

class AndroidLibraryPlugin implements Plugin<Project> {
    private Project project

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: JavaBasePlugin

        def buildTypes = project.container(BuildType)

        project.extensions.create('android', AndroidLibraryExtension, buildTypes)

        buildTypes.whenObjectAdded { BuildType buildType ->
            addBuildType(buildType)
        }
        buildTypes.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing build types is not implemented yet.")
        }

        buildTypes.create('debug')
        buildTypes.create('release')
    }

    void addBuildType(BuildType buildType) {
        def assemble = project.tasks.add("assemble${buildType.name.capitalize()}")

        project.tasks.assemble.dependsOn assemble
    }
}
