package org.gradle.android.internal

import org.gradle.api.file.FileCollection

/**
 * Represents something that can be packaged into an APK and installed.
 */
public interface ApplicationVariant {
    String getName()

    String getDescription()

    String getDirName()

    String getBaseName()

    boolean getZipAlign()

    FileCollection getRuntimeClasspath()

    FileCollection getResourcePackage()
}
