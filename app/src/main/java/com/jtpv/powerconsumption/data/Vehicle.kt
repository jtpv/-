package com.jtpv.powerconsumption.data

/**
 * 车辆配置与档案。
 *
 * 注意：插电混动车型（尤其秦 PLUS DM-i）各年款的电池容量与油箱容积差异很大，
 * 因此这两项必须作为「用户可配置项」，并配合年款快捷预设，不可在代码中硬编码。
 *
 * 档案类字段（车牌、到期日、保养间隔）用于提醒模块。到期日一律以
 * yyyy-MM-dd 文本存储：一是排序与比较天然正确，二是空串即可表达「未设置」，
 * 无需再引入可空列。
 */
data class Vehicle(
    var id: Long = 0L,
    var name: String = "我的车",
    var batteryKwh: Double = 0.0,
    var tankLiter: Double = 0.0,
    var presetIndex: Int = PRESET_CUSTOM,

    // ===== 档案 =====
    var plateNo: String = "",
    var modelName: String = "",
    var purchaseDate: String = "",
    var odoCurrent: Long = 0L,

    // ===== 到期日（空串表示未设置） =====
    var insuranceExpiry: String = "",
    var inspectionExpiry: String = "",

    // ===== 保养规则（0 表示不启用该项提醒） =====
    var maintIntervalKm: Long = DEFAULT_MAINT_KM,
    var maintIntervalMonth: Int = DEFAULT_MAINT_MONTH
) {
    companion object {
        /** 与 VehiclePresets 中最后一项、strings.xml 的 preset_names 最后一项对应 */
        const val PRESET_CUSTOM = 6

        /**
         * 默认保养间隔。
         * 取 10000 km / 12 个月 —— 插混车型因发动机启停频繁，保养周期通常短于
         * 纯燃油车，但也不宜按 5000 km 过度提醒。用户可在档案页按保养手册调整。
         */
        const val DEFAULT_MAINT_KM = 10000L
        const val DEFAULT_MAINT_MONTH = 12
    }
}

/**
 * 年款预设参数表。
 * 索引与 res/values/strings.xml 中的 preset_names 数组严格一一对应。
 *
 * 参数依据车型资料：
 *  - 初代 / 荣耀版：电池 8.32 / 18.32 kWh，油箱 48 L
 *  - 2025 智驾版 ：电池 7.68 / 15.80 kWh，油箱 65 L
 * 调用方需保证 preset_names 与本表的顺序、条目数一致。
 */
object VehiclePresets {

    val batteryKwh = doubleArrayOf(
        8.32,   // 0 初代 55km
        18.32,  // 1 初代 120km
        8.32,   // 2 荣耀版 55km
        18.32,  // 3 荣耀版 120km
        7.68,   // 4 2025 智驾版 55km
        15.80,  // 5 2025 智驾版 120km
        0.0     // 6 自定义
    )

    val tankLiter = doubleArrayOf(
        48.0,   // 0
        48.0,   // 1
        48.0,   // 2
        48.0,   // 3
        65.0,   // 4
        65.0,   // 5
        0.0     // 6 自定义
    )

    /** 索引越界或指向最后一项时视为「自定义」 */
    fun isCustom(index: Int): Boolean =
        index < 0 || index >= batteryKwh.size - 1
}
