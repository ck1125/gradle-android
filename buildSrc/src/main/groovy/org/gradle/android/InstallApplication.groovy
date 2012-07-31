package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class InstallApplication extends DefaultTask {
    @Input
    File sdkDir

    @InputFile
    File packageFile

    @TaskAction
    void generate() {
        project.exec {
            executable = new File(getSdkDir(), "platform-tools/adb")
            args 'install'
            args getPackageFile()
        }
    }
}
