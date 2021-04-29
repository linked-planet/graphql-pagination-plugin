package ut

import com.linkedplanet.plugin.graphqlplugin.*
  
data class TestQueryEdge(
    override val node: TestQuery,
    override val cursor: String
) : Edge<TestQuery>

fun Edge<TestQuery>.fix(): TestQueryEdge = this as TestQueryEdge

data class TestQueryConnection(
    override val countTotal: Int,
    override val edges: List<TestQueryEdge>,
    override val pageInfo: PageInfo
) : Connection<TestQuery>

fun Connection<TestQuery>.fix(): TestQueryConnection = this as TestQueryConnection
