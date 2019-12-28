[previous](bulk-indexing.md) | [parent](index.md) | [next](query-dsl.md)
---

# Search

```kotlin
// lets use a slightly different model class this time
data class Thing(val title: String)
```

Lets index some documents to look for ...

```kotlin
// force ES to commit everything to disk so search works right away
thingDao.bulk(refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE) {
    index("1", Thing("The quick brown fox"))
    index("2", Thing("The quick brown emu"))
    index("3", Thing("The quick brown gnu"))
    index("4", Thing("Another thing"))
    5.rangeTo(100).forEach {
        index("$it", Thing("Another thing: $it"))
    }
}
```

## Searching

```kotlin
// a SearchRequest is created and passed into the block
val results = thingDao.search {
    // we can use templating
    val text = "brown"
    source("""
        {
            "query": {
                "match": {
                    "title": {
                        "query": "$text"
                    }
                }
            }
        }
    """.trimIndent())
}
println("Found ${results.totalHits}")

// we can get the deserialized thing from the search response
results.mappedHits.forEach {
    // kotlin sequences are lazy so nothing gets deserialized until you use the sequence
    println(it)
}

// we can also get the underlying `SearchHit` that Elasticsearch returns
results.searchHits.first().apply {
    // this would be useful if we wanted to do some bulk updates
    println("Hit: $id $seqNo $primaryTerm\n$sourceAsString")
}

// or we can get both as a `Pair`
results.hits.first().apply {
    val (searchHit,deserialized) = this
    println("Hit: ${searchHit.id} deserialized from\n ${searchHit.sourceAsString}\nto\n$deserialized")
}
```

Output:

```
Found 3
Thing(title=The quick brown fox)
Thing(title=The quick brown emu)
Thing(title=The quick brown gnu)
Hit: 1 -2 0
{"title":"The quick brown fox"}
Hit: 1 deserialized from
 {"title":"The quick brown fox"}
to
Thing(title=The quick brown fox)

```

## Count

We can also query just to get a document count.

```kotlin
println("The total number of documents is ${thingDao.count()}")

// like with search, we can pass in a JSON query
val query = "quick"
val count = thingDao.count {
    source("""
        {
            "query": {
                "match": {
                    "title": {
                        "query": "$query"
                    }
                }
            }
        }                        
    """.trimIndent())
}
println("We found $count results matching $query")
```

Output:

```
The total number of documents is 100
We found 3 results matching quick

```

## Scrolling searches

Elasticsearch has a notion of scrolling searches for retrieving large amounts of 
documents from an index. Normally this works by keeping track of a scroll token and
passing that to Elasticsearch to fetch subsequent pages of results. Scrolling is useful if
you want to process large amounts of results.

To make scrolling easier and less tedious, the search method on the dao has a simpler solution: simply
set `scrolling` to `true`.
 
A classic use case for using scrolls is to bulk update your documents. You can do this as follows. 

```kotlin
thingDao.bulk {
    // simply set scrolling to true will allow us to scroll over the entire index
    // this will scale no matter what the size of your index is. If you use scrolling,
    // you can also set the ttl for the scroll (default is 1m)
    val results = thingDao.search(scrolling = true,scrollTtlInMinutes = 10) {
        source("""
            {
                "size": 10,
                "query": {
                    "match_all": {}
                }
            }
        """.trimIndent())
    }
    results.hits.forEach { (hit,thing) ->
        if(thing!=null) {
            // we dig out the meta data we need for optimistic locking from the search response
            update(hit.id, hit.seqNo,hit.primaryTerm, thing) { currentThing ->
                currentThing.copy(title="updated thing")
            }
        }
    }
    // after the last page of results, the scroll is cleaned up with an extra request
}
```


---

[previous](bulk-indexing.md) | [parent](index.md) | [next](query-dsl.md)

This Markdown is Generated from Kotlin code. Please don't edit this file and instead edit the [source file](https://github.com/jillesvangurp/es-kotlin-wrapper-client/tree/master/src/test/kotlin/io/inbot/eskotlinwrapper/manual/SearchManualTest.kt) from which this page is generated.