## What is this?

A prototype Gradle plugin to build Android applications. This is intended to be used to explore how such a plugin
would look and to develop some ideas about how such a plugin would be implemented.

The plugin is functional, if a bit rough, and can generate packaged applications ready to install.

## DSL

The plugin adds 2 concepts to the Gradle DSL:

* A _build type_. There are 2 predefined build types, called `release` and `debug`. You can add additional build types.
* A _product flavor_. For example, a free or a paid-for flavour.

If you do not define any flavors for your product, a default flavor called `main` is added.

From this, the plugin will add the appropriate tasks to build each combination of build type and product flavor. The
plugin will also define the following source directories:

* `src/main/java` - Java source to be included in all application variants.
* `src/main/res` - Resources to be included in all application variants.
* `src/main/AndroidManifest.xml' - The application manifest (currently shared by all application variants).
* `src/$BuildType/java` - Java source to be included in all application variants with the given build type.
* `src/$BuildType/res` - Java source to be included in all application variants with the given build type.
* `src/$ProductFlavor/java` - Resources to be included in all application variants with the given product flavor.
* `src/$ProductFlavor/res` - Resources to be included in all application variants with the given product flavor.

You can configure these locations by configuring the associated source set.

Compile time dependencies are declared in the usual way.

Have a look at the `basic/build.gradle` and `customized/build.gradle` build files to see the DSL in action.

## Contents

The source tree contains the following:

* The `buildSrc` directory contains the plugin implementation.
* The `basic` directory contains a simple application that follows the conventions
* The `customized` directory contains an application with some custom build types, product flavors and other
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
* `assemble$ProductFlavor$BuildType` - build the given application variant.
* `install$ProductFlavor$BuildType` - build and install the given application variant.

## Implementation

For each variant (product-flavor, build-type):

* Generates resource source files into `build/source` from resource directories (main-source-set, product-flavor-source-set, build-type-source-set)
* Compile source files (main-source-set, product-flavor-source-set, build-type-source-set, generated-source).
* Converts the bytecode into `build/libs`
* Crunches resources in `build/resources`
* Packages the resource into `build/libs`
* Assembles the application package into `build/libs`.

Some other notes:
* Uses `sourceSets.main.compileClasspath` as the compile classpath for each variant. Could potentially also include
`sourceSets.$BuildType.compileClasspath` and `sourceSets.$ProductFlavor.compileClasspath` as well.
* Currently, the plugin signs all applications using the debug key.
* No support for building test applications.
* No support for building library projects.
* No support for running ProGuard.
