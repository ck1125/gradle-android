package org.gradle.android.internal

import org.gradle.android.ProductFlavor
import org.gradle.api.tasks.SourceSet

class ProductFlavorDimension {
    final ProductFlavor productFlavor
    final Set<AndroidAppVariant> variants = []
    final SourceSet mainSource
    final SourceSet testSource
    AndroidAppVariant debugVariant

    ProductFlavorDimension(ProductFlavor productFlavor, SourceSet mainSource, SourceSet testSource) {
        this.productFlavor = productFlavor
        this.mainSource = mainSource
        this.testSource = testSource
    }

    String getName() {
        return productFlavor.name
    }

    String getAssembleTaskName() {
        return "assemble${productFlavor.name.capitalize()}"
    }
}
