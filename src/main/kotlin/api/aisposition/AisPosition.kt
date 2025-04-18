package org.example.api.aisposition

import kotlinx.serialization.Serializable
import org.example.annotations.*
import org.example.config.Config.BASE_URL
import org.example.constants.McpConstant
import org.example.model.BaseApiInfo
import org.example.model.BaseSchema
import org.example.util.HttpUtil
import org.example.util.toJsonString

@Serializable
class AisPositionInfoSchema: BaseSchema() {
    @Description("水上移动业务标识码（Maritime Mobile Service Identity）")
    @Optional(true)
    val mmsi: String = ""
}

@UniqueId("getAisPositionInfo")
@ApiName("获取船舶信息")
@ClassDescription("根据水上移动业务标识码（Maritime Mobile Service Identity）mmsi查询船舶信息")
@Category(McpConstant.POSITION)
class AisPositionInfo: BaseApiInfo() {
    companion object {
        fun getBaseApiInfo(): BaseApiInfo {
            val api = AisPositionInfo()
            api.init(AisPositionInfoSchema()) { schema ->
                schema as AisPositionInfoSchema
                HttpUtil.get("$BASE_URL/api/Aisposition/getInfo/${schema.mmsi}")
            }
            return api
        }
    }
}

class AisPositionListSchema: BaseSchema() {
    @Description("是否分页")
    @Optional(false)
    val ifPage: Boolean = true

    @Description("当前页码")
    @Optional(false)
    val currentPage: Int = 1

    @Description("每页记录数")
    @Optional(false)
    val pageRecord: Int = 10

    @Description("关键字模糊查询")
    @Optional(true)
    val keyword: String = ""
}

@UniqueId("getAisPositionList")
@ApiName("获取船舶列表信息")
@ClassDescription("根据船舶名称、mmsi、呼号等关键字进行模糊查询")
@Category(McpConstant.POSITION)
class AisPositionList: BaseApiInfo() {
    companion object {
        fun getBaseApiInfo(): BaseApiInfo {
            val api = AisPositionList()
            api.init(AisPositionListSchema()) { schema ->
                schema as AisPositionListSchema
                HttpUtil.post("$BASE_URL/api/Aisposition/getList", schema.toJsonString())
            }
            return api
        }
    }
}