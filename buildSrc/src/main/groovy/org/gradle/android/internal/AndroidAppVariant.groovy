package org.gradle.android.internal

import org.gradle.android.BuildType
import org.gradle.android.ProductFlavor
import org.gradle.api.file.FileCollection

class AndroidAppVariant {
    final String name
    final BuildType buildType
    final ProductFlavor productFlavor
    FileCollection runtimeClasspath

    AndroidAppVariant(BuildType buildType, ProductFlavor productFlavor) {
        this.name = "${productFlavor.name.capitalize()}${buildType.name.capitalize()}"
        this.buildType = buildType
        this.productFlavor = productFlavor
    }

    String getAssembleTaskName() {
        return "assemble$name"
    }

    String getDirName() {
        return "$productFlavor.name/$buildType.name"
    }
}
