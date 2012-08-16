package org.gradle.android

class BuildType {
    final String name

    boolean zipAlign = true

    BuildType(String name) {
        this.name = name
    }
}
