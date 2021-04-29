package ut

import com.linkedplanet.plugin.graphqlplugin.*
  
data class TestPropertyEdge(
    override val node: TestProperty,
    override val cursor: String
) : Edge<TestProperty>

fun Edge<TestProperty>.fix(): TestPropertyEdge = this as TestPropertyEdge

data class TestPropertyConnection(
    override val countTotal: Int,
    override val edges: List<TestPropertyEdge>,
    override val pageInfo: PageInfo
) : Connection<TestProperty>

fun Connection<TestProperty>.fix(): TestPropertyConnection = this as TestPropertyConnection
