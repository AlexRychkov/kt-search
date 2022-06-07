@file:Suppress("unused")

package com.jillesvangurp.searchdsls.mappingdsl

import com.jillesvangurp.jsondsl.JsonDsl
import com.jillesvangurp.jsondsl.JsonDslMarker
import com.jillesvangurp.jsondsl.PropertyNamingConvention
import kotlin.reflect.KProperty

@JsonDslMarker
class IndexSettings : JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    var replicas: Int by property("index.number_of_replicas")
    var shards: Int by property("index.number_of_shards")

    private fun indexObject(type: String, name: String, block: JsonDsl.() -> Unit) {
        val analysis = get("analysis") as JsonDsl? ?: JsonDsl()
        val objects = analysis[type] as JsonDsl? ?: JsonDsl()
        val objectProperties = JsonDsl()
        block.invoke(objectProperties)
        objects[name] = objectProperties
        analysis[type] = objects
        put("analysis", analysis)
    }

    fun addAnalyzer(name: String, block: JsonDsl.() -> Unit) {
        indexObject("analyzer", name, block)
    }

    fun addTokenizer(name: String, block: JsonDsl.() -> Unit) {
        indexObject("tokenizer", name, block)
    }

    fun addCharFilter(name: String, block: JsonDsl.() -> Unit) {
        indexObject("char_filter", name, block)
    }

    fun addFilter(name: String, block: JsonDsl.() -> Unit) {
        indexObject("filter", name, block)
    }
}

@JsonDslMarker
class FieldMappingConfig(typeName: String) : JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    var type: String by property()
    var boost by property<Double>()
    var docValues by property<Boolean>()
    var store by property<Boolean>()
    var enabled by property<Boolean>()
    var copyTo: List<String> by property()

    var analyzer: String by property()
    var searchAnalyzer: String by property()

    init {
        type = typeName
    }

    fun fields(block: FieldMappings.() -> Unit) {
        val fields = this["fields"] as FieldMappings? ?: FieldMappings()
        block.invoke(fields)
        this["fields"] = fields
    }
}

@Suppress("MemberVisibilityCanBePrivate")
@JsonDslMarker
class FieldMappings : JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    fun text(name: String) = field(name, "text") {}
    fun text(property: KProperty<*>) = field(property.name, "text") {}
    fun text(name: String, block: FieldMappingConfig.() -> Unit) = field(name, "text", block)
    fun text(property: KProperty<*>, block: FieldMappingConfig.() -> Unit) = field(property.name, "text", block)
    fun keyword(name: String) = field(name, "keyword") {}
    fun keyword(property: KProperty<*>) = field(property.name, "keyword") {}
    fun keyword(name: String, block: FieldMappingConfig.() -> Unit) = field(name, "keyword", block)
    fun keyword(property: KProperty<*>, block: FieldMappingConfig.() -> Unit) = field(property.name, "keyword", block)
    fun bool(name: String) = field(name, "boolean") {}
    fun bool(property: KProperty<*>) = field(property.name, "boolean") {}
    fun bool(name: String, block: FieldMappingConfig.() -> Unit) = field(name, "boolean", block)
    fun bool(property: KProperty<*>, block: FieldMappingConfig.() -> Unit) = field(property.name, "boolean", block)
    fun date(name: String) = field(name, "date")
    fun date(property: KProperty<*>) = field(property.name, "date")
    fun date(name: String, block: FieldMappingConfig.() -> Unit) = field(name, "date", block)
    fun date(property: KProperty<*>, block: FieldMappingConfig.() -> Unit) = field(property.name, "date", block)

    fun geoPoint(name: String) = field(name, "geo_point")
    fun geoPoint(property: KProperty<*>) = field(property.name, "geo_point")
    fun geoPoint(name: String, block: FieldMappingConfig.() -> Unit) = field(name, "geo_point", block)
    fun geoPoint(property: KProperty<*>, block: FieldMappingConfig.() -> Unit) = field(property.name, "geo_point", block)

    fun geoShape(name: String) = field(name, "geo_shape")
    fun geoShape(property: KProperty<*>) = field(property.name, "geo_shape")
    fun geoShape(name: String, block: FieldMappingConfig.() -> Unit) = field(name, "geo_shape", block)
    fun geoShape(property: KProperty<*>, block: FieldMappingConfig.() -> Unit) = field(property.name, "geo_shape", block)

    inline fun <reified T : Number> number(name: String) = number<T>(name) {}
    inline fun <reified T : Number> number(property: KProperty<*>) = number<T>(property.name) {}

    inline fun <reified T : Number> number(name: String, noinline block: FieldMappingConfig.() -> Unit) {
        val type = when (T::class) {
            Long::class -> "long"
            Int::class -> "integer"
            Float::class -> "float"
            Double::class -> "double"
            else -> throw IllegalArgumentException("unsupported type ${T::class} explicitly specify type")
        }
        field(name, type, block)
    }
    inline fun <reified T : Number> number(property: KProperty<*>, noinline block: FieldMappingConfig.() -> Unit) = number<T>(property.name, block)

    fun objField(name: String, block: FieldMappings.() -> Unit) {
        field(name, "object") {
            val fieldMappings = FieldMappings()
            block.invoke(fieldMappings)
            if (fieldMappings.size > 0) {
                this["properties"] = fieldMappings
            }
        }
    }
    fun objField(property: KProperty<*>, block: FieldMappings.() -> Unit) = objField(property.name, block)

    fun nestedField(name: String, block: FieldMappings.() -> Unit) {
        field(name, "nested") {
            val fieldMappings = FieldMappings()
            block.invoke(fieldMappings)
            if (fieldMappings.size > 0) {
                this["properties"] = fieldMappings
            }
        }
    }
    fun nestedField(property: KProperty<*>, block: FieldMappings.() -> Unit) = nestedField(property.name, block)

    fun field(name: String, type: String) = field(name, type) {}
    fun field(property: KProperty<*>, type: String) = field(property.name, type) {}

    fun field(name: String, type: String, block: FieldMappingConfig.() -> Unit) {
        val mapping = FieldMappingConfig(type)
        block.invoke(mapping)
        put(name, mapping, PropertyNamingConvention.AsIs)
    }
    fun field(property: KProperty<*>, type: String, block: FieldMappingConfig.() -> Unit) = field(property.name, type, block)
}

class IndexSettingsAndMappingsDSL (private val generateMetaFields: Boolean=false) : JsonDsl(namingConvention = PropertyNamingConvention.ConvertToSnakeCase) {
    private var settings by property<IndexSettings>()
    private var mappings by property<JsonDsl>()
//    private var dynamicEnabled by property<Boolean>(customPropertyName = "dynamic")

    fun settings(block: IndexSettings.() -> Unit) {
        val settingsMap = IndexSettings()
        block.invoke(settingsMap)

        settings = settingsMap
    }

    fun meta(block: JsonDsl.() -> Unit) {
        val newMeta = JsonDsl()
        block.invoke(newMeta)
        if(containsKey("mappings")) {
            mappings["_meta"] = newMeta
        } else {
            mappings=JsonDsl().apply { this["_meta"] = newMeta }
        }
    }

    fun mappings(dynamicEnabled: Boolean? = null, block: FieldMappings.() -> Unit) {
        val properties = FieldMappings()
        if(!containsKey("mappings")) {
            mappings = JsonDsl()
        }
        dynamicEnabled?.let {
            mappings["dynamic"] = dynamicEnabled
        }
        block.invoke(properties)
        mappings["properties"] = properties
    }
}