package org.example.model

import kotlinx.serialization.Serializable
import org.example.annotations.*
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

open class BaseApiInfo() {
    var id = ""
    var name = ""
    var description = ""
    var category = ""
    var schema: BaseSchema = BaseSchema()
    var handler: (BaseSchema) -> String = { _ ->
        ""
    }

    fun init(schema: BaseSchema, function: (schema: BaseSchema) -> String) {
        this.id = (this::class.annotations.find { it is UniqueId } as? UniqueId)?.uniqueId ?: ""
        this.name = (this::class.annotations.find { it is ApiName } as? ApiName)?.name ?: ""
        this.description = (this::class.annotations.find { it is ClassDescription } as? ClassDescription)?.description ?: ""
        this.category = (this::class.annotations.find { it is Category } as? Category)?.category ?: ""
        this.schema = schema
        this.handler = function
    }
}

@Serializable
open class BaseSchema() {
}

class ParamInfo {
    var name = ""
    var type = ""
    var description = ""
    var required = false
    var defaultValue: Any? = null
    var source: String = ""

    constructor()

    constructor(property: KProperty<*>, defaultValue: Any?) {
        property.isAccessible = true
        val field = property.javaField
        this.name = property.name
        this.type = (property.returnType.classifier as kotlin.reflect.KClass<*>).simpleName ?: ""
        this.description = field?.getAnnotation(Description::class.java)?.description ?: ""
        this.required = field?.getAnnotation(Optional::class.java)?.optional ?: false
        this.defaultValue = defaultValue
        this.source = field?.getAnnotation(Source::class.java)?.source ?: ""
    }

    override fun toString(): String {
        return "ParamInfo(name='$name', type='$type', description='$description', required=$required, defaultValue=$defaultValue, source=$source)"
    }
}