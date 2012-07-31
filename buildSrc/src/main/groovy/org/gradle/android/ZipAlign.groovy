package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class ZipAlign extends DefaultTask {
    @OutputFile
    File outputFile

    @Input
    File sdkDir

    @InputFile
    File inputFile

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "tools/zipalign")
            args '-f', '4'
            args getInputFile()
            args getOutputFile()
        }
    }
}
