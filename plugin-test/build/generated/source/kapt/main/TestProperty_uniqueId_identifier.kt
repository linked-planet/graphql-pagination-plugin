package ut

import com.linkedplanet.plugin.graphqlplugin.*
import com.apurebase.kgraphql.schema.dsl.types.*
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder

fun TestProperty.toCursor(): String =
    encodeCursor(this, { t -> t.uniqueId.toString()})
    
fun TestProperty.Companion.fromCursor(cursor: String): Int =
    decodeCursor(cursor, { t -> Int.parse(t) })
    
fun TestProperty.Companion.paginatedQuery(
                schemaBuilder: SchemaBuilder,
                queryName: String,
                default: Int,
                getResults: suspend (Int, Int)->List<TestProperty>): Unit {
    schemaBuilder.query(queryName) {
        resolver { first: Int, after: String? ->
            getResults(first, after?.let{ TestProperty.fromCursor(it) } ?: default)
        }
    }
}

fun TestProperty.Companion.cursorProperty(typeDsl: TypeDSL<TestProperty>): Unit {
    typeDsl.property<String>("cursor") {
        resolver { testProperty ->
            testProperty.toCursor()
        }
    }
}

fun List<TestProperty>.paginateInMemory(first: Int?, after: String?): TestPropertyConnection {
    return paginateInMemory(
        this,
        first,
        after,
        TestProperty::toCursor,
        { n, c -> TestPropertyEdge(n, c) },
        { c, e, p -> TestPropertyConnection(c, e.map { it.fix() }, p) }
    ).fix()
}

fun <T: Any> TestProperty.Companion.connectionProperty(
                        typeDsl: TypeDSL<T>,
                        propertyName: String, 
                        toResults: suspend (T)->List<TestProperty>): Unit {
    typeDsl.property<TestPropertyConnection>(propertyName) {
        resolver { t, first: Int?, after: String? ->
            toResults(t).paginateInMemory(
                first,
                after
            ) 
        }
    }
}

fun <T: Any> TestProperty.Companion.connectionProperty(
                        typeDsl: TypeDSL<T>,
                        propertyName: String, 
                        toResults: suspend (T,Int?,Int?)->List<TestProperty>): Unit {
    typeDsl.property<TestPropertyConnection>(propertyName) {
        resolver { t, first: Int?, after: String? ->
            toResults(t, first, after?.let { TestProperty.fromCursor(it) }).paginateInMemory(
                first,
                after
            ) 
        }
    }
}

fun TestProperty.Companion.registerTypes(s: SchemaBuilder): Unit {
    s.type<TestPropertyEdge>() 
    s.type<TestPropertyConnection>() 
}