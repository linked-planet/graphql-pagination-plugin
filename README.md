[![Snapshot](https://github.com/linked-planet/graphql-pagination-plugin/actions/workflows/gradle-publish-snapshot.yml/badge.svg?branch=master)](https://github.com/linked-planet/graphql-pagination-plugin/actions/workflows/gradle-publish-snapshot.yml)
[![Release](https://github.com/linked-planet/graphql-pagination-plugin/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/linked-planet/graphql-pagination-plugin/actions/workflows/gradle-publish.yml)
# GraphQL-pagination-plugin
Plugin for the Kotlin compiler to generate boilerplate code for graphql-pagination
for use with [kgraphql](https://kgraphql.io/).

The generated pagination is implemented according to the [graphQL-best practice](https://graphql.org/learn/pagination/).

## Project setup
### Gradle 
Besides the compile dependency

_build.gradle.kts_
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://artifactory.link-time.org/artifactory/libs-release-public") }
}

dependencies {
    implementation(group = "com.apurebase", name = "kgraphql", version = kGraphQLVersion)
    compile(group = "com.linked-planet.plugin", name = "graphql-plugin", version = graphQLPluginVersion)
}
```
using this plugin will require letting the compiler know it is supposed to use it.
To allow for this, we need to determine the location of its jar-file.

_build.gradle.kts_
```kotlin
fun classpathOf(dependency: String): File? {
    val regex = Regex(".*${dependency.replace(':', '-')}.*")
    return project.configurations.compile.get().firstOrNull { regex.matches(it.name) }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jvmTarget
    kotlinOptions.freeCompilerArgs = listOf(
        "-Xplugin=${classpathOf("graphql-plugin:$graphQLPluginVersion")}",
        "-P", "plugin:arrow.meta.plugin.compiler:generatedSrcOutputDir=${buildDir}"
    )
}
```

## Usage
Usage of this is pretty simple, each data class to be paginated needs two things:
  * `@Paginated` annotation
  * a property annotated with `@Identifier`
  * a `companion object` (may also be empty)

```kotlin
import com.linkedplanet.plugin.graphqlplugin.*

@Paginated
data class Example(
    @Identifier val uniqueId: Int, 
    val someNumber: Int
    ) { companion object }
```

The Property chosen with `@Identifier` will influence the type signature of all functions
that externalize the pagination (i.e. SQL-Queries).

Now you can extend your schema with the generated functionality:

### Connection Properties
_In memory pagination_
```kotlin
KGraphQL.schema { 
    type<PageInfo>() // part of com.linkedplanet.plugin.graphqlplugin
    Example.registerTypes(this)
    type<SomeType>() {
        Example.connectionProperty(
            this,
            "exampleConnections", 
            { parent: SomeType -> ExampleProvider.getExamplesForParent(parent) }// :(SomeType)->List<Example>
        )
    }
    
    //...
}
```

_Externalized pagination_
```kotlin
KGraphQL.schema { 
    type<PageInfo>() // part of com.linkedplanet.plugin.graphqlplugin
    Example.registerTypes(this)
    type<SomeType>() {
        Example.connectionProperty(
            this,
            "exampleConnections", 
            { parent: SomeType, first: Int?, after: Int? -> 
                ExampleProvider.getPaginatedExamplesForParent(parent, first, after) 
            }// :(SomeType, Int?, Type of the Identifier-annotated Property)->List<Example>
        )
    }
    
    //...
}
```



Now, given a query that returns an instance of `SomeType`, let's call it `something` 
we can make use of the paginated property.

```graphql
{
    something {
        exampleConnections(first: 2, after: "Mg==") {
            edges {
                cursor
                node {
                    someNumber
                }
            }
            pageInfo {
                lastCursor
                lastPage
            }    
            totalCount
        }
    }
} 
```

### Paginated Queries
The usage of paginated queries produces a bit different schema, as direct access to the queried
items is preferable to producing a connection-like schema.

Usage however is not more complicated because of this.

```kotlin
KGraphQL.schema {
    type<PageInfo>() // part of com.linkedplanet.plugin.graphqlplugin
    Example.registerTypes(this)
    type<Example>() {
        Example.cursorProperty(this) // provides a property 'cursor' on each object for use with the after-parameter

        property<Int>("totalCount") { // Not necessarily useful as it'd be repeated for each entry
            resolver { _ ->
                ExampleProvider.getExampleCount()
            }
        }
    }
    
    Example.paginatedQuery(
        this, 
        "allExamples", 
        0) // 0 is the default value for the parameter if no after-parameter is provided
    { first: Int, after: Int ->
        ExampleProvider.getExamples
    }
    //...
}
```

With this snippet, the query is fully usable
```graphql
{
    allExamples(first: 1, after: "MQ==") {
        cursor
        someNumber
        totalCount
    }
} 
```

### Alternative 
One can also use the generated `paginateInMemory` function:
```kotlin
fun List<Example>.paginateInMemory(first: Int?, after: String?): ExampleConnection
```

It is more generally useful as it can also be used to build paginated queries in the 
schema. However, it also exposes one of the generated Types directly (`ExampleConnection`).


### On Identifiers
Currently only values of type `Int` or `String` are fully supported to be annotated as Identifier.
One can, however, easily extend the support by providing a parse function on the target-type:
```kotlin
fun TargetType.Companion.parse(s: String): TargetType = TODO()
```

The only requirement to this function is that 
```kotlin
val target: TargetType // = ...
TargetType.parse(target.toString()) == target
```
must hold.

## To come

 - [X] Paginated queries helper
 - [ ] Non-connection type pagination (no edges or pageInfo)