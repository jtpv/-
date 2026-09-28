package com.jtpv.powerconsumption.data

/** 统计结果 */
data class Stats(
    val avgEv: Double = 0.0,
    val avgFuel: Double = 0.0,
    val sumCharge: Double = 0.0,
    val sumFuel: Double = 0.0,
    val sumCost: Double = 0.0
)

object Calc {

    /**
     * 计算口径（依据插电混动车能耗统计规范）：
     *
     *   纯电电耗 = Σ充电量 ÷ EV里程增量 × 100    单位 kWh/100km
     *   亏电油耗 = Σ加油量 ÷ HEV里程增量 × 100    单位 L/100km
     *
     * 之所以分开算：DM-i 仪表盘提供 EV 里程与 HEV 里程两个独立计数器，
     * 二者相加等于总里程。若改用总里程作分母，电耗会被纯电里程稀释而系统性偏低。
     */
    fun stats(records: List<Record>): Stats {
        val charges = records.filter { it.isCharge }
        val fuels = records.filter { !it.isCharge }

        val sumCharge = charges.fold(0.0) { acc, r -> acc + r.amount }
        val sumFuel = fuels.fold(0.0) { acc, r -> acc + r.amount }
        val sumCost = records.fold(0.0) { acc, r -> acc + r.cost }

        val evDelta = counterDelta(charges.map { it.odoEv })
        val hevDelta = counterDelta(fuels.map { it.odoHev })

        return Stats(
            avgEv = if (evDelta > 0L) sumCharge / evDelta * 100.0 else 0.0,
            avgFuel = if (hevDelta > 0L) sumFuel / hevDelta * 100.0 else 0.0,
            sumCharge = sumCharge,
            sumFuel = sumFuel,
            sumCost = sumCost
        )
    }

    /**
     * 计数器增量 = 有效值的最大值 − 最小值。
     * 里程计数器是累计值，只有两个及以上有效读数才能求出区间增量。
     */
    private fun counterDelta(values: List<Long>): Long {
        val valid = values.filter { it > 0L }
        if (valid.size < 2) return 0L
        return valid.max() - valid.min()
    }
}
