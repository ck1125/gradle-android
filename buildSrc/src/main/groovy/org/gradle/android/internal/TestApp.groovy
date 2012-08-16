package org.gradle.android.internal

import org.gradle.android.ProductFlavor
import org.gradle.api.file.FileCollection

class TestApp implements Packagable {
    final String name
    final ProductFlavor productFlavor
    FileCollection runtimeClasspath
    FileCollection resourcePackage

    TestApp(ProductFlavor productFlavor) {
        this.name = "${productFlavor.name.capitalize()}Test"
        this.productFlavor = productFlavor
    }

    @Override
    String getDescription() {
        return "$productFlavor.name test"
    }

    String getDirName() {
        return "${productFlavor.name}/test"
    }

    String getBaseName() {
        return "$productFlavor.name-test"
    }

    @Override
    boolean getZipAlign() {
        return false
    }
}
