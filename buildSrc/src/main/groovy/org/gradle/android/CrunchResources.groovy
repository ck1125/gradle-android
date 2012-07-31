package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class CrunchResources extends DefaultTask {
    @OutputDirectory
    File outputDir

    @Input
    File sdkDir

    @InputFiles
    Iterable<File> sourceDirectories

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "platform-tools/aapt")
            args 'crunch'
            args '-C', getOutputDir()
            getSourceDirectories().each {
                args '-S', it
            }
        }
    }
}
