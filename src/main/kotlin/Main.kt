package org.example

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.util.collections.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.*
import org.example.model.BaseApiInfo
import org.example.util.McpUtil
import org.example.util.toJsonString

/**
 * Start sse-server mcp on port 3001.
 *
 * @param args
 * - "--stdio": Runs an MCP server using standard input/output.
 * - "--sse-server-ktor <port>": Runs an SSE MCP server using Ktor plugin (default if no argument is provided).
 * - "--sse-server <port>": Runs an SSE MCP server with a plain configuration.
 */
fun main(args: Array<String>) {
    McpUtil.init()

    val command = args.firstOrNull() ?: "--sse-server-ktor"
    val port = args.getOrNull(1)?.toIntOrNull() ?: 3001
    when (command) {
        "--stdio" -> runMcpServerUsingStdio()
        "--sse-server-ktor" -> runSseMcpServerUsingKtorPlugin(port)
        "--sse-server" -> runSseMcpServerWithPlainConfiguration(port)
        else -> {
            System.err.println("Unknown command: $command")
        }
    }
}

fun configureServer(): Server {
    val server = Server(
        Implementation(
            name = "mcp-kotlin test server",
            version = "0.1.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true),
            )
        )
    )

    // 1. 基础元工具：获取所有可用API列表
    server.addTool(
        name = "listAllApis",
        description = "列出所有可用的API接口",
        inputSchema = Tool.Input()
    ) {
        try {
            val groupedApis = mutableMapOf<String, MutableList<String>>()

            McpUtil.apis.values.forEach { api ->
                val category = api.category
                val displayText = "${api.id}-${api.name}-${api.description}"
                groupedApis.getOrPut(category) { mutableListOf() }.add(displayText)
            }

            val responseBuilder = StringBuilder("可用API列表(apiId-Name-Description):\n\n")
            groupedApis.forEach { (category, list) ->
                responseBuilder.append("$category:\n")
                list.forEach { responseBuilder.append("  • $it\n") }
                responseBuilder.append("\n")
            }

            responseBuilder.append("可使用 getApiDetails 工具获取特定API的详细信息,其中source表示来源")

            CallToolResult(content = listOf(TextContent(responseBuilder.toString())))
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("错误: 获取API列表失败 - ${e.message}")))
        }
    }

    // 2. 基础元工具：获取特定API的详细信息
    server.addTool(
        name = "getApiDetails",
        description = "获取特定API的详细信息和使用方法",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("apiId") {
                    put("type", "string")
                    put("description", "API的唯一标识符")
                }
            },
            required = listOf("apiId")
        )
    ) { request ->
        val apiId = request.arguments["apiId"]?.jsonPrimitive?.content
            ?: return@addTool CallToolResult(content = listOf(TextContent("错误: 缺少参数 apiId")))

        try {
            val apiInfo = McpUtil.apis.getOrDefault(apiId, BaseApiInfo())
            run {
                val jsonInfo = McpUtil.getApiDefinitionInfo(apiInfo).toJsonString()
                CallToolResult(content = listOf(TextContent("API详细信息:\n$jsonInfo")))
            }
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("错误: 获取API详情失败 - ${e.message}")))
        }
    }

    // 3. 基础元工具：执行指定API
    server.addTool(
        name = "executeApi",
        description = "执行指定的API,需要提供API的ID和参数",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                putJsonObject("apiId") {
                    put("type", "string")
                    put("description", "要执行的API的唯一标识符")
                }
                putJsonObject("params") {
                    put("type", "object")
                    put("description", "API的参数")
                }
            },
            required = listOf("apiId")
        )
    ) { request ->
        val apiId = request.arguments["apiId"]?.jsonPrimitive?.content
        val params = request.arguments["params"]?.let { jsonElement ->
            if (jsonElement is JsonPrimitive)
                jsonElement.jsonPrimitive.content
            else
                jsonElement.toString()
        } ?: "{}"

        if (apiId == null) {
            return@addTool CallToolResult(content = listOf(TextContent("错误: 缺少参数 apiId")))
        }

        try {
            val result = McpUtil.apis[apiId]?.let { api ->
                val schema = Gson().fromJson(params, api.schema::class.java)
                api.handler(schema)
            } ?: "API未找到"

            CallToolResult(content = listOf(TextContent(result)))
        } catch (e: Exception) {
            CallToolResult(content = listOf(TextContent("错误: ${e.message}")))
        }
    }

    return server
}

fun runMcpServerUsingStdio() {
    // Note: The server will handle listing prompts, tools, and resources automatically.
    // The handleListResourceTemplates will return empty as defined in the Server code.
    val server = configureServer()
    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
        println("Server closed")
    }
}

fun runSseMcpServerWithPlainConfiguration(port: Int): Unit = runBlocking {
    val servers = ConcurrentMap<String, Server>()
    println("Starting sse server on port $port. ")
    println("Use inspector to connect to the http://localhost:$port/sse")

    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        install(SSE)
        routing {
            sse("/sse") {
                val transport = SseServerTransport("/message", this)
                val server = configureServer()

                // For SSE, you can also add prompts/tools/resources if needed:
                // server.addTool(...), server.addPrompt(...), server.addResource(...)

                servers[transport.sessionId] = server

                server.onClose {
                    println("Server closed")
                    servers.remove(transport.sessionId)
                }

                server.connect(transport)
            }
            post("/message") {
                println("Received Message")
                val sessionId: String = call.request.queryParameters["sessionId"]!!
                val transport = servers[sessionId]?.transport as? SseServerTransport
                if (transport == null) {
                    call.respond(HttpStatusCode.NotFound, "Session not found")
                    return@post
                }

                transport.handlePostMessage(call)
            }
        }
    }.start(wait = true)
}

/**
 * Starts an SSE (Server Sent Events) MCP server using the Ktor framework and the specified port.
 *
 * The url can be accessed in the MCP inspector at [http://localhost:$port]
 *
 * @param port The port number on which the SSE MCP server will listen for client connections.
 * @return Unit This method does not return a value.
 */
fun runSseMcpServerUsingKtorPlugin(port: Int): Unit = runBlocking {
    println("Starting sse server on port $port")
    println("Use inspector to connect to the http://localhost:$port/sse")

    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        mcp {
            return@mcp configureServer()
        }
    }.start(wait = true)
}
