package com.jtpv.powerconsumption.data

/**
 * 一条补能记录（充电 或 加油）。
 *
 * 里程字段说明（对应 DM-i 仪表盘）：
 *  - odoTotal：总里程
 *  - odoEv   ：EV 里程累计值（纯电行驶）
 *  - odoHev  ：HEV 里程累计值（混动行驶）
 *  EV 与 HEV 是两个独立计数器，相加等于总里程，因此可分别精确统计电耗与油耗，
 *  不应使用「总里程作分母」的折算口径——那会被纯电里程稀释而系统性偏低。
 */
data class Record(
    var id: Long = 0L,
    var vehicleId: Long = 1L,
    var type: String = TYPE_CHARGE,
    var date: String = "",
    var amount: Double = 0.0,
    var price: Double = 0.0,
    var cost: Double = 0.0,
    var odoTotal: Long = 0L,
    var odoEv: Long = 0L,
    var odoHev: Long = 0L,
    var isFull: Boolean = false,
    var note: String = "",
    /**
     * 每公里费用（展示用，不入库）：本次费用 ÷ 与上次同类型记录的里程差。
     * 充电看 EV 里程差、加油看 HEV 里程差——与电耗/油耗统计口径一致。
     * 由 Repository.listRecords 填充，首条记录或里程未填时为 0。
     */
    var perKmCost: Double = 0.0
) {
    val isCharge: Boolean
        get() = type == TYPE_CHARGE

    companion object {
        const val TYPE_CHARGE = "CHARGE"
        const val TYPE_FUEL = "FUEL"
    }
}
