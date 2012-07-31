package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GeneratePackage extends DefaultTask {
    @OutputFile
    File outputFile

    @Input
    File sdkDir

    @InputFile
    File resourceFile

    @InputFile
    File dexFile

    @TaskAction
    void generate() {
        def antJar = new File(getSdkDir(), "tools/lib/anttasks.jar")
        ant.taskdef(resource: "anttasks.properties", classpath: antJar)
        ant.apkbuilder(apkFilepath: getOutputFile(),
                resourcefile: getResourceFile().getName(),
                outfolder: getOutputFile().getParentFile(),
                debugsigning: true) {
            dex(path: getDexFile())
        }
    }
}
