package org.gradle.android

class ProductFlavor {
    final String name
    String packageName
    Integer versionCode
    String versionName

    ProductFlavor(String name) {
        this.name = name
    }
}

