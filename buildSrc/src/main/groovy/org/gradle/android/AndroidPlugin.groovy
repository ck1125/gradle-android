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
    private GenerateResources generateSourceTask
    private File sdkDir
    private AndroidExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: JavaBasePlugin

        def buildTypes = project.container(BuildType)
        def productFlavors = project.container(ProductFlavor)

        extension = project.extensions.create('android', AndroidExtension, buildTypes, productFlavors)

        findSdk(project)

        generateSourceTask = project.tasks.add('generateSource', GenerateResources)
        generateSourceTask.conventionMapping.outputDir = { project.file("$project.buildDir/source") }
        generateSourceTask.sdkDir = sdkDir
        generateSourceTask.sourceDirectories = [project.file('src/main/res')].findAll {it.exists() }
        generateSourceTask.androidManifestFile = project.file('src/main/AndroidManifest.xml')
        generateSourceTask.conventionMapping.includeFiles = { [getRuntimeJar()] }

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

        buildTypes.add(new BuildType('debug'))
        buildTypes.add(new BuildType('release'))

        project.afterEvaluate {
            if (productFlavors.isEmpty()) {
                productFlavors.add(new ProductFlavor('main'))
            }
        }

        project.tasks.assemble.dependsOn { variants.collect { it.assembleTaskName} }
    }

    private File getRuntimeJar() {
        def platformDir = new File(sdkDir, "platforms/${extension.target}")
        if (!platformDir.exists()) {
            throw new RuntimeException("Specified target '$extension.target' does not exist.")
        }
        new File(platformDir, "android.jar")
    }

    private void findSdk(Project project) {
        def localProperties = project.file("local.properties")
        if (!localProperties) {
            throw new RuntimeException("No local.properties file found at ${localProperties}.")
        }
        Properties properties = new Properties()
        localProperties.withInputStream { instr ->
            properties.load(instr)
        }
        def sdkDirProp = properties.getProperty('sdk.dir')
        if (!sdkDirProp) {
            throw new RuntimeException("No sdk.dir property defined in local.properties file.")
        }
        sdkDir = new File(sdkDirProp)
        if (!sdkDir.directory) {
            throw new RuntimeException("The SDK directory '$sdkDir' specified in local.properties does not exist.")
        }
    }

    private void addBuildType(BuildType buildType) {
        def sourceSet = project.sourceSets.add(buildType.name)

        def buildTypeDimension = new BuildTypeDimension(buildType, sourceSet)
        buildTypes[buildType.name] = buildTypeDimension

        def assembleBuildType = project.tasks.add(buildTypeDimension.assembleTaskName)
        assembleBuildType.dependsOn {
            buildTypeDimension.variants.collect { it.assembleTaskName }
        }
        assembleBuildType.description = "Assembles all ${buildType.name} applications"
        assembleBuildType.group = "Build"

        productFlavors.values().each { flavor ->
            addVariant(buildTypeDimension, flavor)
        }
    }

    private void addProductFlavor(ProductFlavor productFlavor) {
        def sourceSet
        if (productFlavor.name == 'main') {
            sourceSet = main
        } else {
            sourceSet = project.sourceSets.add(productFlavor.name)
        }

        def productFlavorDimension = new ProductFlavorDimension(productFlavor, sourceSet)
        productFlavors[productFlavor.name] = productFlavorDimension

        def assembleFlavour = project.tasks.add(productFlavorDimension.assembleTaskName)
        assembleFlavour.dependsOn {
            productFlavorDimension.variants.collect { it.assembleTaskName }
        }
        assembleFlavour.description = "Assembles all ${productFlavor.name} applications"
        assembleFlavour.group = "Build"

        def testCompile = project.tasks.add("compile${productFlavor.name.capitalize()}Test")

        def testJar = project.tasks.add("test${productFlavor.name.capitalize()}Jar")
        testJar.dependsOn testCompile

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
        compileTask.source main.java, buildType.sourceSet.java, productFlavor.sourceSet.java, generateSourceTask.outputs
        compileTask.classpath = project.files()
        compileTask.conventionMapping.destinationDir = { project.file("$project.buildDir/classes/$variant.classesDirName") }
        // TODO - make this use convention mapping
        compileTask.doFirst {
            options.bootClasspath = getRuntimeJar()
        }

        // Add a jar task
        def jarTaskName = "jar${variant.name}"
        def jarTask = project.tasks.add(jarTaskName, Jar)
        jarTask.from compileTask
        jarTask.conventionMapping.baseName = { "${project.archivesBaseName}-${productFlavor.name}-${buildType.name}" as String }

        // Add an assemble task
        def assembleTask = project.tasks.add(variant.assembleTaskName)
        assembleTask.dependsOn jarTask
        assembleTask.description = "Assembles the ${productFlavor.name} ${buildType.name} application"
        assembleTask.group = "Build"
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

        String getName() {
            return buildType.name
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

        String getName() {
            return productFlavor.name
        }

        String getAssembleTaskName() {
            return "assemble${productFlavor.name.capitalize()}"
        }
    }
}
