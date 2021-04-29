package ut

import com.linkedplanet.plugin.graphqlplugin.*
import com.apurebase.kgraphql.schema.dsl.types.*
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder

fun TestQuery.toCursor(): String =
    encodeCursor(this, { t -> t.uniqueId.toString()})
    
fun TestQuery.Companion.fromCursor(cursor: String): Int =
    decodeCursor(cursor, { t -> Int.parse(t) })
    
fun TestQuery.Companion.paginatedQuery(
                schemaBuilder: SchemaBuilder,
                queryName: String,
                default: Int,
                getResults: suspend (Int, Int)->List<TestQuery>): Unit {
    schemaBuilder.query(queryName) {
        resolver { first: Int, after: String? ->
            getResults(first, after?.let{ TestQuery.fromCursor(it) } ?: default)
        }
    }
}

fun TestQuery.Companion.cursorProperty(typeDsl: TypeDSL<TestQuery>): Unit {
    typeDsl.property<String>("cursor") {
        resolver { testQuery ->
            testQuery.toCursor()
        }
    }
}

fun List<TestQuery>.paginateInMemory(first: Int?, after: String?): TestQueryConnection {
    return paginateInMemory(
        this,
        first,
        after,
        TestQuery::toCursor,
        { n, c -> TestQueryEdge(n, c) },
        { c, e, p -> TestQueryConnection(c, e.map { it.fix() }, p) }
    ).fix()
}

fun <T: Any> TestQuery.Companion.connectionProperty(
                        typeDsl: TypeDSL<T>,
                        propertyName: String, 
                        toResults: suspend (T)->List<TestQuery>): Unit {
    typeDsl.property<TestQueryConnection>(propertyName) {
        resolver { t, first: Int?, after: String? ->
            toResults(t).paginateInMemory(
                first,
                after
            ) 
        }
    }
}

fun <T: Any> TestQuery.Companion.connectionProperty(
                        typeDsl: TypeDSL<T>,
                        propertyName: String, 
                        toResults: suspend (T,Int?,Int?)->List<TestQuery>): Unit {
    typeDsl.property<TestQueryConnection>(propertyName) {
        resolver { t, first: Int?, after: String? ->
            toResults(t, first, after?.let { TestQuery.fromCursor(it) }).paginateInMemory(
                first,
                after
            ) 
        }
    }
}

fun TestQuery.Companion.registerTypes(s: SchemaBuilder): Unit {
    s.type<TestQueryEdge>() 
    s.type<TestQueryConnection>() 
}