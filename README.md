# GraphQL-pagination-plugin
Plugin for the Kotlin compiler to generate boilerplate code for graphql-pagination
for use with [kgraphql](https://kgraphql.io/).

The generated pagination is implemented according to the [graphQL-best practice](https://graphql.org/learn/pagination/).

## Project setup
### Gradle 
Besides the compile dependency

_build.gradle.kts_
```kotlin
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
  * a `companion object` (may also be empty)

```kotlin
import com.linkedplanet.plugin.graphqlplugin.Paginated

@Paginated
data class Example(val uniqueId: Int, val someNumber: Int) { companion object }
```

Now you can extend your schema with the generated functionality:
```kotlin
KGraphQL.schema { 
    type<PageInfo>() // part of com.linkedplanet.plugin.graphqlplugin
    Example.registerTypes(this)
    type<SomeType>() {
        Example.connectionProperty(
            "exampleConnections", 
            { parent: SomeType -> ExampleProvider.getExamplesForParent(parent) }// :(SomeType)->List<Example>,
            // toCursor function, should provide a string that uniquely identifies
            // an element of example
            { example: Example -> "${example.uniqueId}" } 
        )(this)
    }
    
    ...
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

## To come

 - [ ] Paginated queries helper
 - [ ] Non-connection type pagination (no edges or pageInfo)