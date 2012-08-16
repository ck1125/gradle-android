package org.gradle.android

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

class AndroidLibraryExtension {
    final NamedDomainObjectContainer<BuildType> buildTypes

    AndroidLibraryExtension(NamedDomainObjectContainer<BuildType> buildTypes) {
        this.buildTypes = buildTypes
    }

    void buildTypes(Action<? super NamedDomainObjectContainer<BuildType>> action) {
        action.execute(buildTypes)
    }
}
