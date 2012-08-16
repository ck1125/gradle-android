package org.gradle.android

import org.gradle.android.internal.AndroidAppVariant
import org.gradle.android.internal.BuildTypeDimension
import org.gradle.android.internal.ProductFlavorDimension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.Compile
import org.gradle.android.internal.AndroidManifest
import org.gradle.internal.reflect.Instantiator
import org.gradle.android.internal.Packagable
import org.gradle.android.internal.TestApp

class AndroidPlugin implements Plugin<Project> {
    private final Set<AndroidAppVariant> variants = []
    private final Map<String, BuildTypeDimension> buildTypes = [:]
    private final Map<String, ProductFlavorDimension> productFlavors = [:]
    private Project project
    private SourceSet main
    private SourceSet test
    private File sdkDir
    private AndroidExtension extension
    private AndroidManifest mainManifest

    @Override
    void apply(Project project) {
        this.project = project

        project.apply plugin: JavaBasePlugin

        def buildTypes = project.container(BuildType)
        // TODO - do the decoration by default
        def productFlavors = project.container(ProductFlavor) { name ->
            project.services.get(Instantiator).newInstance(ProductFlavor, name)
        }

        extension = project.extensions.create('android', AndroidExtension, buildTypes, productFlavors)
        extension.conventionMapping.packageName = { getMainManifest().packageName }
        extension.conventionMapping.versionCode = { getMainManifest().versionCode }
        extension.conventionMapping.versionName = { getMainManifest().versionName }

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

        buildTypes.create('debug') {
            zipAlign = false
        }
        buildTypes.create('release')

        productFlavors.whenObjectAdded { ProductFlavor flavor ->
            addProductFlavor(flavor)
        }
        productFlavors.whenObjectRemoved {
            throw new UnsupportedOperationException("Removing product flavors is not implemented yet.")
        }

        project.afterEvaluate {
            if (productFlavors.isEmpty()) {
                productFlavors.create('main')
            }
        }

        project.tasks.assemble.dependsOn { variants.collect { "assemble${it.name}" } }
    }

    private File getRuntimeJar() {
        def platformDir = new File(sdkDir, "platforms/${extension.target}")
        if (!platformDir.exists()) {
            throw new RuntimeException("Specified target '$extension.target' does not exist.")
        }
        new File(platformDir, "android.jar")
    }

    private AndroidManifest getMainManifest() {
        if (mainManifest == null) {
            mainManifest = new AndroidManifest()
            mainManifest.load(project.file("src/main/AndroidManifest.xml"))
        }
        return mainManifest
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
            buildTypeDimension.variants.collect { "assemble$it.name" }
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

        productFlavor.conventionMapping.packageName = { extension.packageName }
        productFlavor.conventionMapping.versionCode = { extension.versionCode }
        productFlavor.conventionMapping.versionName = { extension.versionName }

        def assembleFlavour = project.tasks.add(productFlavorDimension.assembleTaskName)
        assembleFlavour.dependsOn {
            productFlavorDimension.variants.collect { "assemble${it.name}" }
        }
        assembleFlavour.description = "Assembles all ${productFlavor.name} applications"
        assembleFlavour.group = "Build"

        buildTypes.values().each { buildType ->
            addVariant(buildType, productFlavorDimension)
        }

        assert productFlavorDimension.debugVariant != null

        // Add a task to generate the test app manifest
        def generateManifestTask = project.tasks.add("generate${productFlavor.name.capitalize()}TestManifest", GenerateManifest)
        generateManifestTask.conventionMapping.outputFile = { project.file("$project.buildDir/manifests/test/$productFlavor.name/AndroidManifest.xml") }
        generateManifestTask.conventionMapping.packageName = { productFlavor.packageName + '.test' }
        generateManifestTask.conventionMapping.versionCode = { productFlavor.versionCode }
        generateManifestTask.conventionMapping.versionName = { productFlavor.versionName }

        // Add a task to compile the test application
        def testCompile = project.tasks.add("compile${productFlavor.name.capitalize()}Test", Compile)
        testCompile.source test.java, productFlavorDimension.testSource.java
        testCompile.classpath = test.compileClasspath + productFlavorDimension.debugVariant.runtimeClasspath
        testCompile.conventionMapping.destinationDir = { project.file("$project.buildDir/classes/test/$productFlavor.name") }
        testCompile.options.bootClasspath = getRuntimeJar()

        // Add a task to generate resource package
        def processResources = project.tasks.add("process${productFlavor.name.capitalize()}TestResources", ProcessResources)
        processResources.dependsOn generateManifestTask
        processResources.conventionMapping.packageFile = { project.file("$project.buildDir/libs/test/${project.archivesBaseName}-${productFlavor.name}.ap_") }
        processResources.sdkDir = sdkDir
        processResources.conventionMapping.sourceDirectories =  { [] }
        processResources.conventionMapping.androidManifestFile = { generateManifestTask.outputFile }
        processResources.conventionMapping.packageName = { generateManifestTask.packageName }
        processResources.conventionMapping.includeFiles = { [getRuntimeJar()] }

        def testApp = new TestApp(productFlavor)
        testApp.runtimeClasspath = testCompile.outputs.files + testCompile.classpath
        testApp.resourcePackage = project.files({processResources.packageFile}) { builtBy processResources }
        addPackageTasks(testApp)

        project.tasks.check.dependsOn "assemble${testApp.name}"
    }

    private void addVariant(BuildTypeDimension buildType, ProductFlavorDimension productFlavor) {
        def variant = new AndroidAppVariant(buildType.buildType, productFlavor.productFlavor)
        variants << variant
        buildType.variants << variant
        productFlavor.variants << variant
        if (buildType.name == 'debug') {
            productFlavor.debugVariant = variant
        }

        // Add a task to generate the manifest
        def generateManifestTask = project.tasks.add("generate${variant.name}Manifest", GenerateManifest)
        generateManifestTask.sourceFile = project.file('src/main/AndroidManifest.xml')
        generateManifestTask.conventionMapping.outputFile = { project.file("$project.buildDir/manifests/main/$variant.dirName/AndroidManifest.xml") }
        generateManifestTask.conventionMapping.packageName = { getMainManifest().packageName }
        generateManifestTask.conventionMapping.versionCode = { productFlavor.productFlavor.versionCode }
        generateManifestTask.conventionMapping.versionName = { productFlavor.productFlavor.versionName }

        // Add a task to crunch resource files
        def crunchTask = project.tasks.add("crunch${variant.name}Resources", CrunchResources)
        crunchTask.conventionMapping.outputDir = { project.file("$project.buildDir/resources/main/$variant.dirName") }
        crunchTask.sdkDir = sdkDir
        crunchTask.conventionMapping.sourceDirectories =  {
            (main.resources.srcDirs + productFlavor.mainSource.resources.srcDirs + buildType.mainSource.resources.srcDirs).findAll { it.exists() }
        }

        // Add a task to generate resource source files
        def processResources = project.tasks.add("process${variant.name}Resources", ProcessResources)
        processResources.dependsOn generateManifestTask, crunchTask
        processResources.conventionMapping.sourceOutputDir = { project.file("$project.buildDir/source/main/$variant.dirName") }
        processResources.conventionMapping.packageFile = { project.file("$project.buildDir/libs/${project.archivesBaseName}-${variant.baseName}.ap_") }
        processResources.sdkDir = sdkDir
        processResources.conventionMapping.sourceDirectories =  {
            ([crunchTask.outputDir] + main.resources.srcDirs + productFlavor.mainSource.resources.srcDirs + buildType.mainSource.resources.srcDirs).findAll { it.exists() }
        }
        processResources.conventionMapping.androidManifestFile = { generateManifestTask.outputFile }
        processResources.conventionMapping.includeFiles = { [getRuntimeJar()] }
        processResources.conventionMapping.packageName = { productFlavor.productFlavor.packageName }

        // Add a compile task
        def compileTaskName = "compile${variant.name}"
        def compileTask = project.tasks.add(compileTaskName, Compile)
        compileTask.dependsOn processResources
        compileTask.source main.java, buildType.mainSource.java, productFlavor.mainSource.java, { processResources.sourceOutputDir }
        compileTask.classpath = main.compileClasspath
        compileTask.conventionMapping.destinationDir = { project.file("$project.buildDir/classes/main/$variant.dirName") }
        compileTask.options.bootClasspath = getRuntimeJar()

        // Wire up the outputs
        variant.runtimeClasspath = project.files(compileTask.outputs, main.compileClasspath)
        variant.resourcePackage = project.files({processResources.packageFile}) { builtBy processResources }

        addPackageTasks(variant)
    }

    private void addPackageTasks(Packagable packagable) {
        // Add a dex task
        def dexTaskName = "dex${packagable.name}"
        def dexTask = project.tasks.add(dexTaskName, Dex)
        dexTask.sdkDir = sdkDir
        dexTask.conventionMapping.sourceFiles = { packagable.runtimeClasspath }
        dexTask.conventionMapping.outputFile = { project.file("${project.buildDir}/libs/${project.archivesBaseName}-${packagable.baseName}.dex") }

        // Add a task to generate application package
        def packageApp = project.tasks.add("package${packagable.name}", PackageApplication)
        packageApp.dependsOn packagable.resourcePackage, dexTask
        packageApp.conventionMapping.outputFile = { project.file("$project.buildDir/apk/${project.archivesBaseName}-${packagable.baseName}-unaligned.apk") }
        packageApp.sdkDir = sdkDir
        packageApp.conventionMapping.resourceFile = { packagable.resourcePackage.singleFile }
        packageApp.conventionMapping.dexFile = { dexTask.outputFile }

        def appTask = packageApp

        if (packagable.zipAlign) {
            // Add a task to zip align application package
            def alignApp = project.tasks.add("zipalign${packagable.name}", ZipAlign)
            alignApp.dependsOn packageApp
            alignApp.conventionMapping.inputFile = { packageApp.outputFile }
            alignApp.conventionMapping.outputFile = { project.file("$project.buildDir/apk/${project.archivesBaseName}-${packagable.baseName}.apk") }
            alignApp.sdkDir = sdkDir

            appTask = alignApp
        }

        // Add an assemble task
        def assembleTask = project.tasks.add("assemble${packagable.name}")
        assembleTask.dependsOn appTask
        assembleTask.description = "Assembles the ${packagable.description} application"
        assembleTask.group = "Build"

        // Add a task to install the application package
        def installApp = project.tasks.add("install${packagable.name}", InstallApplication)
        installApp.dependsOn appTask
        installApp.conventionMapping.packageFile = { appTask.outputFile }
        installApp.sdkDir = sdkDir
    }
}
