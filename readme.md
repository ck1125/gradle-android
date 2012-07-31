## What is this?

A prototype Gradle plugin to build Android applications. This is intended to be used to explore how such a plugin
would look and to develop some ideas about how such a plugin would be implemented.

## DSL

The plugin adds 2 concepts to the Gradle DSL:

* A _build type_. There are 2 predefined build types, called `release` and `debug`. You can add additional build types.
* A _product flavor_. For example, a free or a paid-for flavour.

If you do not define any flavors for your product, a default flavor called `main` is added.

From this, the plugin will add the appropriate tasks to build each combination of build type and product flavor. The
plugin will also define the following source directories:

* `src/main/java` - Java source to be included in all applications.
* `src/main/res` - Resources to be included in all applications
* `src/main/AndroidManifest.xml'
* `src/$BuildType/java`
* `src/$ProductFlavor/java`

You can configure these locations by configuring the associated source set.

Have a look in the `customized/build.gradle` build file to see the DSL in action.

## Contents

The `buildSrc` directory contains the plugin implementation.

The `basic` directory contains a simple application that follows the conventions

The `customized` directory contains an application with some custom build types, product flavors and other
customizations.

## Usage

Before you start, edit the `basic/local.properties` and `customized/local.properties` files to point at your local install
of the Android SDK. Normally, these files would not be checked into source control, but would be generated when the
project is bootstrapped.

Try `./gradlew basic:tasks` in the root directory.

You can also run:

* `assemble` - builds all combinations of build type and product flavor
* `assemble$BuildType` - build all flavors for the given build type.
* `assemble$ProductFlavor` - build all build types for the given product flavor.
* `assemble$ProductFlavor$BuildType` - build the given build type of the given product flavor.

## Implementation

* Generates resource source files into `build/source`
* Compiles each variant (product-flavor, build-type) using source files (main-source-set, product-flavor-source-set, build-type-source-set, generated-source).
* Assembles a jar for each variant into `build/libs`.
