package ut

import com.apurebase.kgraphql.KGraphQL
import com.linkedplanet.plugin.graphqlplugin.*

@Paginated
data class TestProperty(
    @Identifier val uniqueId: Int,
    val someString: String
) {companion object}

@Paginated
data class TestQuery(
    @Identifier val uniqueId: Int,
    val someString: String
) {companion object}

val schema = KGraphQL.schema {
    type<PageInfo>()
    type<TestProperty>()
    TestProperty.registerTypes(this)
    type<TestQuery>() {
        TestQuery.cursorProperty(this)
        TestProperty.connectionProperty(this, "propConnection") { _: TestQuery ->
            listOf(TestProperty(0, "Property"))
        }
    }

    TestQuery.paginatedQuery(this, "allQueries", -1) { first: Int, after: Int ->
        listOf(
            TestQuery(0,"Query 1"),
            TestQuery(1,"Query 2"),
        ).filter { it.uniqueId > after }.take(first)
    }
}
