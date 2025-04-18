package org.example.api.station

import kotlinx.serialization.Serializable
import org.example.annotations.*
import org.example.config.Config.BASE_URL
import org.example.constants.McpConstant
import org.example.model.BaseApiInfo
import org.example.model.BaseSchema
import org.example.util.HttpUtil

class StationStateSchema: BaseSchema() {
}

@UniqueId("getStationState")
@ApiName("获取站点数据源状态信息")
@ClassDescription("查询东保、北保、卫星、基站等数据源的连接状态信息")
@Category(McpConstant.STATION)
class StationState: BaseApiInfo() {
    companion object {
        fun getBaseApiInfo(): BaseApiInfo {
            val api = StationState()
            api.init(StationStateSchema()) { schema ->
                schema as StationStateSchema
                """
                    state: 连接状态
                    hostState: 主机连接状态
                    portState: 端口连接状态
                    abnormalTime: 异常时间
                    ${HttpUtil.get("$BASE_URL/api/Aisposition/getStationState")}
                """.trimIndent()
            }
            return api
        }
    }
}