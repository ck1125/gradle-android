package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class Dex extends DefaultTask {
    @OutputFile
    File outputFile

    @Input
    File sdkDir

    @InputFiles
    Iterable<File> sourceFiles

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "platform-tools/dx")
            args '--dex'
            args '--output', getOutputFile()
            getSourceFiles().each {
                args it
            }
        }
    }
}
