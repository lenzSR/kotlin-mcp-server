import org.example.model.BaseApiInfo
import org.example.util.McpUtil
import org.example.util.toJsonString
import org.junit.jupiter.api.Test

class Test {
    @Test
    fun example() {
        println(McpUtil.getApiDefinitionInfo(McpUtil.apis.getOrDefault("getStationState", BaseApiInfo())).toJsonString())
    }
}