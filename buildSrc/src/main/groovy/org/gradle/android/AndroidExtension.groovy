package org.gradle.android

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

class AndroidExtension {
    final NamedDomainObjectContainer<BuildType> buildTypes
    final NamedDomainObjectContainer<ProductFlavor> productFlavors
    String target = "android-16"

    AndroidExtension(NamedDomainObjectContainer<BuildType> buildTypes, NamedDomainObjectContainer<ProductFlavor> productFlavors) {
        this.buildTypes = buildTypes
        this.productFlavors = productFlavors
    }

    void buildTypes(Action<? super NamedDomainObjectContainer<BuildType>> action) {
        action.execute(buildTypes)
    }

    void productFlavors(Action<? super NamedDomainObjectContainer<ProductFlavor>> action) {
        action.execute(productFlavors)
    }
}
