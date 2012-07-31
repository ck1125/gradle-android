package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.InputFile

class GenerateResources extends DefaultTask {
    @OutputDirectory
    File outputDir

    @Input
    File sdkDir

    @InputFiles
    Iterable<File> sourceDirectories = []

    @InputFiles
    Iterable<File> includeFiles = []

    @InputFile
    File androidManifestFile

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "platform-tools/aapt")
            args 'package'
            args '-f'
            args '-m'
            args '-J', getOutputDir()
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
