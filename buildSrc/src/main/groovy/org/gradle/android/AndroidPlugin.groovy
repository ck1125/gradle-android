package org.gradle.android

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.Compile
import org.gradle.api.tasks.bundling.Jar

class AndroidPlugin implements Plugin<Project> {
    private final Set<AndroidAppVariant> variants = []
    private final Map<String, BuildTypeDimension> buildTypes = [:]
    private final Map<String, ProductFlavorDimension> productFlavors = [:]
    private Project project
    private SourceSet main

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: JavaBasePlugin

        def buildTypes = project.container(BuildType)
        def productFlavors = project.container(ProductFlavor)

        project.extensions.create('android', AndroidExtension, buildTypes, productFlavors)

        main = project.sourceSets.add('main')

        buildTypes.whenObjectAdded { BuildType buildType ->
            addBuildType(buildType)
        }
        buildTypes.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing build types is not implemented yet.")
        }

        productFlavors.whenObjectAdded { ProductFlavor flavor ->
            addProductFlavor(flavor)
        }
        productFlavors.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing product flavors is not implemented yet.")
        }

        project.tasks.assemble.dependsOn { variants.collect{ it.assembleTaskName} }
    }

    private void addBuildType(BuildType buildType) {
        def sourceSet = project.sourceSets.add(buildType.name)

        def buildTypeDimension = new BuildTypeDimension(buildType, sourceSet)
        buildTypes[buildType.name] = buildTypeDimension

        def assembleBuildType = project.tasks.add(buildTypeDimension.assembleTaskName)
        assembleBuildType.dependsOn {
            buildTypeDimension.variants.collect { it.assembleTaskName }
        }

        productFlavors.values().each { flavor ->
            addVariant(buildTypeDimension, flavor)
        }
    }

    private void addProductFlavor(ProductFlavor productFlavor) {
        def sourceSet = project.sourceSets.add(productFlavor.name)

        def productFlavorDimension = new ProductFlavorDimension(productFlavor, sourceSet)
        productFlavors[productFlavor.name] = productFlavorDimension

        def assembleFlavour = project.tasks.add(productFlavorDimension.assembleTaskName)
        assembleFlavour.dependsOn {
            productFlavorDimension.variants.collect { it.assembleTaskName }
        }

        buildTypes.values().each { buildType ->
            addVariant(buildType, productFlavorDimension)
        }
    }

    private void addVariant(BuildTypeDimension buildType, ProductFlavorDimension productFlavor) {
        def variant = new AndroidAppVariant(buildType.buildType, productFlavor.productFlavor)
        variants << variant
        buildType.variants << variant
        productFlavor.variants << variant

        // Add a compile task
        def compileTaskName = "compile${variant.name}"
        def compileTask = project.tasks.add(compileTaskName, Compile)
        compileTask.source main.java, buildType.sourceSet.java, productFlavor.sourceSet.java
        compileTask.classpath = project.files()
        compileTask.conventionMapping.destinationDir = { project.file("$project.buildDir/classes/$variant.classesDirName") }

        // Add a jar task
        def jarTaskName = "jar${variant.name}"
        def jarTask = project.tasks.add(jarTaskName, Jar)
        jarTask.from compileTask
        jarTask.conventionMapping.baseName = { "${project.archivesBaseName}-${productFlavor.productFlavor.name}-${buildType.buildType.name}" as String }

        // Add an assemble task
        def assembleTask = project.tasks.add(variant.assembleTaskName)
        assembleTask.dependsOn jarTask
    }

    private static class AndroidAppVariant {
        final String name
        final BuildType buildType
        final ProductFlavor productFlavor

        AndroidAppVariant(BuildType buildType, ProductFlavor productFlavor) {
            this.name = "${productFlavor.name.capitalize()}${buildType.name.capitalize()}"
            this.buildType = buildType
            this.productFlavor = productFlavor
        }

        String getAssembleTaskName() {
            return "assemble$name"
        }

        String getClassesDirName() {
            return "$productFlavor.name/$buildType.name"
        }
    }

    private static class BuildTypeDimension {
        final BuildType buildType
        final Set<AndroidAppVariant> variants = []
        final SourceSet sourceSet

        BuildTypeDimension(BuildType buildType, SourceSet sourceSet) {
            this.buildType = buildType
            this.sourceSet = sourceSet
        }

        String getAssembleTaskName() {
            return "assemble${buildType.name.capitalize()}"
        }
    }

    private static class ProductFlavorDimension {
        final ProductFlavor productFlavor
        final Set<AndroidAppVariant> variants = []
        final SourceSet sourceSet

        ProductFlavorDimension(ProductFlavor productFlavor, SourceSet sourceSet) {
            this.productFlavor = productFlavor
            this.sourceSet = sourceSet
        }

        String getAssembleTaskName() {
            return "assemble${productFlavor.name.capitalize()}"
        }
    }
}
