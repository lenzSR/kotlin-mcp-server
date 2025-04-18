package org.example.util

import io.github.classgraph.ClassGraph
import org.example.annotations.UniqueId
import org.example.model.BaseApiInfo
import org.example.model.ParamInfo
import org.reflections.Reflections
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object McpUtil {
    val apis = mutableMapOf<String, BaseApiInfo>()

    fun init() {
        val reflections = Reflections("org.example")
        val annotatedClasses = reflections.getTypesAnnotatedWith(UniqueId::class.java)


        for (clazz in annotatedClasses) {
            try {
                // 获取 Kotlin 的 KClass
                val kClass = clazz.kotlin

                val companionClass = kClass.companionObject

                if (companionClass != null) {
                    // 查找 getBaseApiInfo 函数（无参数）
                    val method = companionClass.declaredFunctions.firstOrNull { it.name == "getBaseApiInfo" }
                    if (method != null) {
                        // 获取 companion object 实例
                        kClass.companionObjectInstance?.let { companionInstance ->
                            method.isAccessible = true // 如果是 private/internal 也能访问
                            val result = method.call(companionInstance)
                            if (result is BaseApiInfo) {
                                apis[result.id] = result
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        log.info("Loaded APIs: ${apis.map { it.value.id }}")
    }

    data class ApiDefinitionInfo (
        val id: String,
        val name: String,
        val description: String,
        val category: String,
        val params: List<ParamInfo>,
        val examples: Any?
    )

    fun getApiDefinitionInfo(apiInfo: BaseApiInfo): ApiDefinitionInfo {
        return ApiDefinitionInfo(
            id = apiInfo.id,
            name = apiInfo.name,
            description = apiInfo.description,
            category = apiInfo.category,
            params = apiInfo.schema.let { schema ->
                schema::class.memberProperties.map { property ->
                    property.isAccessible = true
                    val defaultValue = property.getter.call(schema)
                    ParamInfo(property, defaultValue)
                }
            } ?: mutableListOf(),
            examples = null // 这里可以添加示例数据
        )
    }

}