package org.gradle.android

import org.gradle.android.internal.AndroidAppVariant
import org.gradle.android.internal.BuildTypeDimension
import org.gradle.android.internal.ProductFlavorDimension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.Compile

class AndroidPlugin implements Plugin<Project> {
    private final Set<AndroidAppVariant> variants = []
    private final Map<String, BuildTypeDimension> buildTypes = [:]
    private final Map<String, ProductFlavorDimension> productFlavors = [:]
    private Project project
    private SourceSet main
    private SourceSet test
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

        project.sourceSets.all { sourceSet ->
            sourceSet.resources.srcDirs = ["src/$sourceSet.name/res"]
        }

        main = project.sourceSets.add('main')
        test = project.sourceSets.add('test')

        buildTypes.whenObjectAdded { BuildType buildType ->
            addBuildType(buildType)
        }
        buildTypes.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing build types is not implemented yet.")
        }

        buildTypes.add(new BuildType('debug'))
        buildTypes.add(new BuildType('release'))

        productFlavors.whenObjectAdded { ProductFlavor flavor ->
            addProductFlavor(flavor)
        }
        productFlavors.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing product flavors is not implemented yet.")
        }

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
        def mainSourceSet
        def testSourceSet
        if (productFlavor.name == 'main') {
            mainSourceSet = main
            testSourceSet = test
        } else {
            mainSourceSet = project.sourceSets.add(productFlavor.name)
            testSourceSet = project.sourceSets.add("test${productFlavor.name.capitalize()}")
        }

        def productFlavorDimension = new ProductFlavorDimension(productFlavor, mainSourceSet, testSourceSet)
        productFlavors[productFlavor.name] = productFlavorDimension

        def assembleFlavour = project.tasks.add(productFlavorDimension.assembleTaskName)
        assembleFlavour.dependsOn {
            productFlavorDimension.variants.collect { it.assembleTaskName }
        }
        assembleFlavour.description = "Assembles all ${productFlavor.name} applications"
        assembleFlavour.group = "Build"

        buildTypes.values().each { buildType ->
            addVariant(buildType, productFlavorDimension)
        }

        assert productFlavorDimension.debugVariant != null

        def testCompile = project.tasks.add("compile${productFlavor.name.capitalize()}Test", Compile)
        testCompile.source test.java, productFlavorDimension.testSource.java
        testCompile.classpath = test.compileClasspath + productFlavorDimension.debugVariant.runtimeClasspath
        testCompile.conventionMapping.destinationDir = { project.file("$project.buildDir/test-classes/$productFlavor.name") }
        // TODO - make this use convention mapping
        testCompile.doFirst {
            options.bootClasspath = getRuntimeJar()
        }
    }

    private void addVariant(BuildTypeDimension buildType, ProductFlavorDimension productFlavor) {
        def variant = new AndroidAppVariant(buildType.buildType, productFlavor.productFlavor)
        variants << variant
        buildType.variants << variant
        productFlavor.variants << variant
        if (buildType.name == 'debug') {
            productFlavor.debugVariant = variant
        }

        // Add a task to generate resource source files
        def generateSourceTask = project.tasks.add("generate${variant.name}Source", GenerateResourceSource)
        generateSourceTask.conventionMapping.outputDir = { project.file("$project.buildDir/source/$variant.dirName") }
        generateSourceTask.sdkDir = sdkDir
        generateSourceTask.conventionMapping.sourceDirectories =  {
            (main.resources.srcDirs + productFlavor.mainSource.resources.srcDirs + buildType.mainSource.resources.srcDirs).findAll { it.exists() }
        }
        generateSourceTask.androidManifestFile = project.file('src/main/AndroidManifest.xml')
        generateSourceTask.conventionMapping.includeFiles = { [getRuntimeJar()] }

        // Add a compile task
        def compileTaskName = "compile${variant.name}"
        def compileTask = project.tasks.add(compileTaskName, Compile)
        compileTask.source main.java, buildType.mainSource.java, productFlavor.mainSource.java, generateSourceTask.outputs
        compileTask.classpath = main.compileClasspath
        compileTask.conventionMapping.destinationDir = { project.file("$project.buildDir/classes/$variant.dirName") }
        // TODO - make this use convention mapping
        compileTask.doFirst {
            options.bootClasspath = getRuntimeJar()
        }

        // Wire up the runtime classpath
        variant.runtimeClasspath = project.files(compileTask.outputs, main.compileClasspath)

        // Add a dex task
        def dexTaskName = "dex${variant.name}"
        def dexTask = project.tasks.add(dexTaskName, Dex)
        dexTask.sdkDir = sdkDir
        dexTask.conventionMapping.sourceFiles = { variant.runtimeClasspath }
        dexTask.conventionMapping.outputFile = { project.file("${project.buildDir}/libs/${project.archivesBaseName}-${productFlavor.name}-${buildType.name}.dex") }

        // Add a task to crunch resource files
        def crunchTask = project.tasks.add("crunch${variant.name}Resources", CrunchResources)
        crunchTask.conventionMapping.outputDir = { project.file("$project.buildDir/resources/$variant.dirName") }
        crunchTask.sdkDir = sdkDir
        crunchTask.conventionMapping.sourceDirectories =  {
            (main.resources.srcDirs + productFlavor.mainSource.resources.srcDirs + buildType.mainSource.resources.srcDirs).findAll { it.exists() }
        }

        // Add a task to generate resource package
        def generateResources = project.tasks.add("package${variant.name}Resources", GenerateResourcePackage)
        generateResources.dependsOn crunchTask
        generateResources.conventionMapping.outputFile = { project.file("$project.buildDir/libs/${project.archivesBaseName}-${productFlavor.name}-${buildType.name}.ap_") }
        generateResources.sdkDir = sdkDir
        generateResources.conventionMapping.sourceDirectories =  {
            ([crunchTask.outputDir] + main.resources.srcDirs + productFlavor.mainSource.resources.srcDirs + buildType.mainSource.resources.srcDirs).findAll { it.exists() }
        }
        generateResources.androidManifestFile = project.file('src/main/AndroidManifest.xml')
        generateResources.conventionMapping.includeFiles = { [getRuntimeJar()] }

        // Add a task to generate application package
        def packageApp = project.tasks.add("package${variant.name}", PackageApplication)
        packageApp.dependsOn generateResources, dexTask
        packageApp.conventionMapping.outputFile = { project.file("$project.buildDir/libs/${project.archivesBaseName}-${productFlavor.name}-${buildType.name}-unaligned.apk") }
        packageApp.sdkDir = sdkDir
        packageApp.conventionMapping.resourceFile = { generateResources.outputFile }
        packageApp.conventionMapping.dexFile = { dexTask.outputFile }

        // Add a task to zip align application package
        def alignApp = project.tasks.add("zipalign${variant.name}", ZipAlign)
        alignApp.dependsOn packageApp
        alignApp.conventionMapping.inputFile = { packageApp.outputFile }
        alignApp.conventionMapping.outputFile = { project.file("$project.buildDir/libs/${project.archivesBaseName}-${productFlavor.name}-${buildType.name}.apk") }
        alignApp.sdkDir = sdkDir

        // Add an assemble task
        def assembleTask = project.tasks.add(variant.assembleTaskName)
        assembleTask.dependsOn alignApp
        assembleTask.description = "Assembles the ${productFlavor.name} ${buildType.name} application"
        assembleTask.group = "Build"

        // Add a task to install the application package
        def installApp = project.tasks.add("install${variant.name}", InstallApplication)
        installApp.dependsOn alignApp
        installApp.conventionMapping.packageFile = { alignApp.outputFile }
        installApp.sdkDir = sdkDir
    }
}
