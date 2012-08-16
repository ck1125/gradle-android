package org.gradle.android.internal

import org.gradle.api.file.FileCollection

/**
 * Represents something that can be packaged into an APK.
 */
public interface Packagable {
    String getName()

    String getDescription()

    String getDirName()

    String getBaseName()

    FileCollection getRuntimeClasspath()

    FileCollection getResourcePackage()
}
