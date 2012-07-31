
## What is this?

A prototype Gradle plugin to build Android applications. This is intended to be used to explore how such a plugin
would look and to develop some ideas about how such a plugin would be implemented.

## DSL

The plugin adds 2 concepts to the Gradle DSL:

* A _build type_. There are 2 predefined build types, called `release` and `debug`. You can add additional build types.
* A _product flavor_. For example, a free or a paid-for flavour.

Currently, you must define at least one flavor for your product.

From this, the plugin will add the appropriate tasks to build each combination of build type and product flavor. The
plugin will also define the following source directories:

* `src/main/java`
* `src/$BuildType/java`
* `src/$ProductFlavor/java`

You can configure these locations by configuring the associated source set.

Have a look in the `build.gradle` in the root directory to see the DSL in action.

## Usage

Try `./gradlew tasks` in the root directory.

You can also run:

* `./gradlew assemble` - builds all combinations of build type and product flavor
* `./gradlew assemble$BuildType` - build all flavors for the given build type.
* `./gradlew assemble$ProductFlavor_` - build all build types for the given product flavor.
* `./gradlew assemble$ProductFlavor$BuildType_` - build the given build type of the given product flavor.

## Implementation

* Generates some source into `build/source`
* Compiles each variant (product-flavor, build-type) using source files (main-source-set, product-flavor-source-set, build-type-source-set, generated-source).
* Assembles a jar for each variant into `build/libs`.
