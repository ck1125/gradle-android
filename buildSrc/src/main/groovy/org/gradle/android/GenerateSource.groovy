package org.gradle.android

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory

class GenerateSource extends DefaultTask {
    @OutputDirectory
    File outputDir

    @TaskAction
    void generate() {
        def sourceFile = new File(getOutputDir(), "org/gradle/sample/Generated.java");
        sourceFile.parentFile.mkdirs()
        sourceFile.text = '''
package org.gradle.sample;

public class Generated {
}
'''
    }
}
