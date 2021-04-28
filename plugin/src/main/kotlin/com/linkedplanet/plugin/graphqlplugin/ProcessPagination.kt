/*
MIT License

Copyright (c) 2021 linked-planet GmbH

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.linkedplanet.plugin.graphqlplugin

import arrow.meta.*
import arrow.meta.phases.analysis.companionObject
import arrow.meta.quotes.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*

/*
  The base directory for the generated files is provided in the build.gradle file
  of the module that is using the plugin ('use-plugin' module):
  compileKotlin {
    kotlinOptions {
      ...
      freeCompilerArgs = [ ...
        "-P", "plugin:arrow.meta.plugin.compiler:generatedSrcOutputDir=${buildDir}"]
    }
  }
  If base directory is not provided, it will be "build" directory in the Gradle daemon workspace.
 */
val Meta.processIdentifier: CliPlugin
    get() = "Generate identifier boilerplate" {
        meta(
            parameter(this, ::isIdentifierProperty) { prop ->
                val clazz = prop.parent.parent.parent as KtClass
                val edgeName = "${clazz.name}Edge"
                val connName = "${clazz.name}Connection"
                Transform.newSources(
                    """|package ${prop.containingKtFile.packageFqName}
                       |
                       |import com.linkedplanet.plugin.graphqlplugin.*
                       |import com.apurebase.kgraphql.schema.dsl.types.*
                       |import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
                       |
                       |fun ${clazz.name}.toCursor(): String =
                       |    encodeCursor(this, { t -> t.${name}.toString()})
                       |    
                       |fun ${clazz.name}.Companion.fromCursor(cursor: String): $type =
                       |    decodeCursor(cursor, { t -> ${type}.parse(t) })
                       |    
                       |fun ${clazz.name}.Companion.paginatedQuery(
                       |                schemaBuilder: SchemaBuilder,
                       |                queryName: String,
                       |                default: $type,
                       |                getResults: suspend (Int, $type)->List<${clazz.name}>): Unit {
                       |    schemaBuilder.query(queryName) {
                       |        resolver { first: Int, after: String? ->
                       |            getResults(first, after?.let{ ${clazz.name}.fromCursor(it) } ?: default)
                       |        }
                       |    }
                       |    schemaBuilder.type<${clazz.name}> {
                       |        property<String>("cursor") {
                       |            resolver { ${clazz.name!!.decapitalize()} ->
                       |                ${clazz.name!!.decapitalize()}.toCursor()
                       |            }
                       |        }
                       |    }
                       |}
                       |
                       |fun List<${clazz.name}>.paginateInMemory(first: Int?, after: String?): $connName {
                       |    return paginateInMemory(
                       |        this,
                       |        first,
                       |        after,
                       |        ${clazz.name}::toCursor,
                       |        { n, c -> $edgeName(n, c) },
                       |        { c, e, p -> $connName(c, e.map { it.fix() }, p) }
                       |    ).fix()
                       |}
                       |
                       |fun <T: Any, C> ${clazz.name}.Companion.connectionProperty(
                       |                        typeDsl: TypeDSL<T>,
                       |                        propertyName: String, 
                       |                        toResults: suspend (T)->List<${clazz.name}>): Unit {
                       |    typeDsl.property<$connName>(propertyName) {
                       |        resolver { t, first: Int?, after: String? ->
                       |            toResults(t).paginateInMemory(
                       |                first,
                       |                after
                       |            ) 
                       |        }
                       |    }
                       |}
                       |
                       |fun <T: Any, C> ${clazz.name}.Companion.connectionProperty(
                       |                        typeDsl: TypeDSL<T>,
                       |                        propertyName: String, 
                       |                        toResults: suspend (T,Int?,${type}?)->List<${clazz.name}>): Unit {
                       |    typeDsl.property<$connName>(propertyName) {
                       |        resolver { t, first: Int?, after: String? ->
                       |            toResults(t, first, after?.let { ${clazz.name}.fromCursor(it) }).paginateInMemory(
                       |                first,
                       |                after
                       |            ) 
                       |        }
                       |    }
                       |}
                       |
                       |fun ${clazz.name}.Companion.registerTypes(s: SchemaBuilder): Unit {
                       |    s.type<$edgeName>() 
                       |    s.type<$connName>() 
                       |}""".trimMargin("|").file("${name}_identifier")
                )
            }
        )
    }

val Meta.processPagination: CliPlugin
    get() = "Generate pagination boilerplate" {
        meta(
            classDeclaration(this, ::isPaginatedClass) { declaration ->
                val edgeName = "${name}Edge"
                val connName = "${name}Connection"
                Transform.newSources(
                    """|package ${declaration.containingKtFile.packageFqName}
                       |
                       |import com.linkedplanet.plugin.graphqlplugin.*
                       |  
                       |data class $edgeName(
                       |    override val node: ${name},
                       |    override val cursor: String
                       |) : Edge<${name}>
                       |
                       |fun Edge<${name}>.fix(): $edgeName = this as $edgeName
                       |
                       |data class $connName(
                       |    override val countTotal: Int,
                       |    override val edges: List<$edgeName>,
                       |    override val pageInfo: PageInfo
                       |) : Connection<${name}>
                       |
                       |fun Connection<${name}>.fix(): $connName = this as $connName
                       |""".trimMargin("|").file("$name")
                )
            }
        )
    }

private fun isPaginatedClass(ktClass: KtClass): Boolean =
    ktClass.isData() &&
            ktClass.annotationEntries.any { it.text.matches(Regex("@Paginated")) } &&
            ktClass.primaryConstructorParameters.isNotEmpty() &&
            ktClass.typeParameters.isEmpty() &&
            ktClass.companionObject != null &&
            ktClass.parent is KtFile

private fun isIdentifierProperty(ktProperty: KtParameter): Boolean =
    ktProperty.annotationEntries.any { it.text.matches(Regex("@Identifier")) } &&
            ktProperty.parent.parent.parent is KtClass &&
            isPaginatedClass(ktProperty.parent.parent.parent as KtClass)
