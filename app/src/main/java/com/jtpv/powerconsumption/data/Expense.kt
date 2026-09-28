package com.jtpv.powerconsumption.data

/**
 * 费用类型常量。
 *
 * 顺序必须与 res/values/strings.xml 中 expense_type_names 数组严格一致 ——
 * 界面按索引取显示名，错位会导致类型名张冠李戴。
 */
object ExpenseType {

    const val MAINTENANCE = "MAINTENANCE"   // 保养
    const val REPAIR = "REPAIR"             // 维修
    const val INSURANCE = "INSURANCE"       // 保险
    const val INSPECTION = "INSPECTION"     // 年检
    const val PARKING = "PARKING"           // 停车
    const val TOLL = "TOLL"                 // 过路费
    const val WASH = "WASH"                 // 洗车美容
    const val FINE = "FINE"                 // 违章罚款
    const val ACCESSORY = "ACCESSORY"       // 装饰配件
    const val OTHER = "OTHER"               // 其他

    /** 顺序与 expense_type_names 一一对应，不可随意调整 */
    val all: List<String> = listOf(
        MAINTENANCE, REPAIR, INSURANCE, INSPECTION, PARKING,
        TOLL, WASH, FINE, ACCESSORY, OTHER
    )

    /** 取类型在显示名数组中的下标；未知类型归入最后一项，保证界面不会越界 */
    fun indexOf(type: String): Int {
        val i = all.indexOf(type)
        return if (i >= 0) i else all.size - 1
    }
}

/**
 * 一条费用记录（补能之外的支出）。
 *
 * 为什么与 [Record] 分表：
 * 补能记录需要里程、单价、「是否加满」这类能耗计算专用字段，其统计口径是
 * kWh/100km 与 L/100km；而保养、保险、停车等只关心金额。混在一张表里会让
 * 能耗统计被迫过滤大量无关行，语义也不清晰。二者汇总后构成用车总成本。
 */
data class Expense(
    var id: Long = 0L,
    var vehicleId: Long = 1L,
    var type: String = ExpenseType.MAINTENANCE,
    var date: String = "",
    var amount: Double = 0.0,
    var odo: Long = 0L,
    var shop: String = "",
    var note: String = ""
) {
    val isMaintenance: Boolean
        get() = type == ExpenseType.MAINTENANCE
}
