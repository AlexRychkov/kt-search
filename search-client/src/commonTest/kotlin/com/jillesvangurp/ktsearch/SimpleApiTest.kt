package com.jillesvangurp.ktsearch

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class SimpleApiTest : SearchTest()  {

    @Test
    fun clusterShouldBeHealthy() = coTest {
        client.clusterHealth().status shouldNotBe ClusterStatus.Red
    }

    @Test
    fun createIndex() = coTest {
        val response = client.createIndex(randomIndexName()) {
            mappings(false) {
                keyword("foo")
                number<Long>("bar")
            }
            meta {
                this["foo"] = "bar"
            }
            settings {
                replicas=0
                shards=5
            }
        }
        println(response.getOrThrow().text)
    }
}