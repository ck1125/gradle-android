package org.gradle.android.internal

import org.gradle.android.BuildType
import org.gradle.android.ProductFlavor
import org.gradle.api.file.FileCollection

class AndroidAppVariant implements Packagable {
    final String name
    final BuildType buildType
    final ProductFlavor productFlavor
    FileCollection runtimeClasspath
    FileCollection resourcePackage

    AndroidAppVariant(BuildType buildType, ProductFlavor productFlavor) {
        this.name = "${productFlavor.name.capitalize()}${buildType.name.capitalize()}"
        this.buildType = buildType
        this.productFlavor = productFlavor
    }

    String getDescription() {
        return "$productFlavor.name $buildType.name"
    }

    String getDirName() {
        return "$productFlavor.name/$buildType.name"
    }

    String getBaseName() {
        return "$productFlavor.name-$buildType.name"
    }

    @Override
    boolean getZipAlign() {
        return buildType.zipAlign
    }
}
