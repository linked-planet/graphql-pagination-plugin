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

import java.util.*

data class PageInfo(val lastCursor: String?, val lastPage: Boolean)

interface Edge<T> {
    val node: T
    val cursor: String
}

interface Connection<T> {
    val countTotal: Int
    val edges: List<Edge<T>>
    val pageInfo: PageInfo
}

/**
 * Function that provides in-memory pagination for any list of 'Paginated'-annotated objects.
 */
fun <T> paginate(
    results: List<T>,
    first: Int?,
    after: String?,
    toCursor: (T) -> String,
    edgeConstructor: (T, String) -> Edge<T>,
    connectionConstructor: (Int, List<Edge<T>>, PageInfo) -> Connection<T>
): Connection<T> {
    val decodedAfter = after?.let {
        Base64.getDecoder().decode(it).map { b -> b.toChar() }.joinToString()
    }
    val offset =
        if (decodedAfter != null) results.dropWhile { decodedAfter != toCursor(it) }.drop(1)
        else results
    val shortened =
        if (first != null) offset.take(first)
        else offset
    val edges = shortened.map { item ->
        edgeConstructor(item, Base64.getEncoder().encodeToString(toCursor(item).toByteArray()))
    }
    return connectionConstructor(
        results.size,
        edges,
        PageInfo(edges.lastOrNull()?.cursor, first?.let { offset.size <= it } ?: true)
    )
}

/**
 * Requires the presence of a companion object to work.
 * Will generate boilerplate to allow pagination in graphQL
 * according to the declared best practice.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class Paginated
