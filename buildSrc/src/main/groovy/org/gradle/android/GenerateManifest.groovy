package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputFile
import org.gradle.android.internal.AndroidManifest

class GenerateManifest extends DefaultTask {
    @InputFile
    File sourceFile

    @OutputFile
    File outputFile

    @Input
    String packageName

    @Input
    Integer versionCode

    @Input
    String versionName

    @TaskAction
    def generate() {
        AndroidManifest manifest = new AndroidManifest()
        manifest.load(getSourceFile())
        manifest.packageName = getPackageName()
        manifest.versionCode = getVersionCode()
        manifest.versionName = getVersionName()
        manifest.save(getOutputFile())
    }
}

