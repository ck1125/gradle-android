package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateResourcePackage extends DefaultTask {
    @OutputFile
    File outputFile

    @Input
    File sdkDir

    @InputFiles
    Iterable<File> sourceDirectories

    @InputFiles
    Iterable<File> includeFiles

    @InputFile
    File androidManifestFile

    @Input
    String packageName

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "platform-tools/aapt")
            args 'package'
            args '-f'
            args '--debug-mode'
            args '--no-crunch'
            args '--generate-dependencies'
            args '--rename-manifest-package', getPackageName()
            args '-F', getOutputFile()
            args '-M', getAndroidManifestFile()
            getSourceDirectories().each {
                args '-S', it
            }
            getIncludeFiles().each {
                args '-I', it
            }
        }
    }
}
