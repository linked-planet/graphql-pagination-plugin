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
                       |import com.apurebase.kgraphql.schema.dsl.types.*
                       |import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
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
                       |
                       |fun List<${name}>.paginate(first: Int?, after: String?, toCursor: (${name}) -> String): $connName {
                       |    return paginate(
                       |        this,
                       |        first,
                       |        after,
                       |        toCursor,
                       |        { n, c -> $edgeName(n, c) },
                       |        { c, e, p -> $connName(c, e.map { it.fix() }, p) }
                       |    ).fix()
                       |}
                       |
                       |fun <T: Any> ${name}.Companion.connectionProperty(
                       |                        propertyName: String, 
                       |                        toResults: suspend (T)->List<${name}>, 
                       |                        toCursor: (${name})->String): TypeDSL<T>.()->Unit = {
                       |    property<$connName>(propertyName) {
                       |        resolver { t, first: Int?, after: String? ->
                       |            toResults(t).paginate(
                       |                first,
                       |                after,
                       |                toCursor
                       |            ) 
                       |        }
                       |    }
                       |}
                       |
                       |fun ${name}.Companion.registerTypes(s: SchemaBuilder): Unit {
                       |    s.type<$edgeName>() 
                       |    s.type<$connName>() 
                       |}""".trimMargin("|").file("$name")
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