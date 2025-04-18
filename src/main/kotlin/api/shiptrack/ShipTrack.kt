package org.example.api.shiptrack

import org.example.annotations.*
import org.example.config.Config.BASE_URL
import org.example.constants.McpConstant
import org.example.model.BaseApiInfo
import org.example.model.BaseSchema
import org.example.util.HttpUtil
import org.example.util.toJsonString

class ShipTrackSchema: BaseSchema() {
    @Description("起始时间（yyyy-MM-dd HH:mm:ss）")
    @Optional(true)
    val startTime: String = ""

    @Description("结束时间（yyyy-MM-dd HH:mm:ss）")
    @Optional(true)
    val endTime: String = ""

    @Description("水上移动业务标识码（Maritime Mobile Service Identity）")
    @Optional(true)
    val mmsi: String = ""
}

@UniqueId("getShipTrack")
@ApiName("获取单条船舶的历史轨迹")
@ClassDescription("根据起始时间、结束时间和mmsi获取单条船舶的历史轨迹")
@Category(McpConstant.SHIP_TRACK)
class ShipTrack: BaseApiInfo() {
    companion object {
        fun getBaseApiInfo(): BaseApiInfo {
            val api = ShipTrack()
            api.init(ShipTrackSchema()) { schema ->
                schema as ShipTrackSchema
                HttpUtil.post("$BASE_URL/api/Aisposition/getHistoryList", schema.toJsonString())
            }
            return api
        }
    }
}