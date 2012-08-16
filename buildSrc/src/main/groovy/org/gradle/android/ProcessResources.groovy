package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Optional

class ProcessResources extends DefaultTask {
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

    @OutputDirectory @Optional
    File sourceOutputDir

    @OutputFile
    File packageFile

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "platform-tools/aapt")
            args 'package'
            args '-f'
            args '-m'
            args '--generate-dependencies'
            args '--rename-manifest-package', getPackageName()
            if (getSourceOutputDir() != null) {
                args '-J', getSourceOutputDir()
            }
            args '-F', getPackageFile()
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
