package ut

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.*


class GenerationTest: StringSpec({

    "TestQuery should have pagination" {
        val result = schema.execute("{allQueries(first:1){someString cursor}}")
        result shouldContain "Query 1"
        result shouldNotContain "Query 2"
    }

    "TestQuery should accept offset" {
        val result = schema.execute("{allQueries(first:1, after: \"MA==\"){someString cursor}}")
        result shouldNotContain "Query 1"
        result shouldContain "Query 2"
    }

    "TestProperty should have pagination" {
        val result = schema.execute("{allQueries(first:2){someString propConnection(first:1) { edges { cursor node {someString}} pageInfo { lastCursor } }}}")
        result shouldContain "Query 1"
        result shouldContain "Query 2"
        result shouldContain "Property1"
        result shouldNotContain "Property2"
    }

    "TestProperty should accept offset" {
        val result = schema.execute("{allQueries(first:2){someString propConnection(first:1, after: \"MA==\") { edges { node {someString}} }}}")
        result shouldContain "Query 1"
        result shouldContain "Query 2"
        result shouldNotContain "Property1"
        result shouldContain "Property2"
    }
})
