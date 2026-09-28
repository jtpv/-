package com.jtpv.powerconsumption.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 能耗统计结果 */
data class Stats(
    val avgEv: Double = 0.0,
    val avgFuel: Double = 0.0,
    val sumCharge: Double = 0.0,
    val sumFuel: Double = 0.0,
    val sumCost: Double = 0.0
)

/** 单月费用。补能与其余支出分开存放，便于堆叠柱状图区分来源 */
data class MonthCost(
    val label: String,
    val fuel: Double = 0.0,
    val other: Double = 0.0
) {
    val total: Double get() = fuel + other
}

/** 费用构成中的一项 */
data class TypeCost(val type: String, val amount: Double)

/** 用车成本汇总 */
data class CostSummary(
    val chargeCost: Double = 0.0,
    val fuelCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val odoTotal: Long = 0L,
    val costPerKm: Double = 0.0,
    val monthAvg: Double = 0.0,
    val months: List<MonthCost> = emptyList(),
    val byType: List<TypeCost> = emptyList()
)

object Calc {

    /** 报表展示的月份跨度 */
    const val MONTHS = 6

    /**
     * 能耗计算口径（依据插电混动车能耗统计规范）：
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
     * 用车成本汇总。
     *
     * 三项口径说明：
     *  1. 补能费用取自 Record.cost，充电与加油分开统计 —— 便于判断用电与用油的经济性
     *  2. 其他费用取自 Expense.amount
     *  3. 每公里成本 = 总费用 ÷ 里程增量。里程增量沿用 Record.odoTotal 的
     *     「最大值 − 最小值」，与能耗统计同源，避免与车机读数不一致
     */
    fun costSummary(
        records: List<Record>,
        expenses: List<Expense>,
        today: LocalDate
    ): CostSummary {
        val chargeCost = records.filter { it.isCharge }.fold(0.0) { a, r -> a + r.cost }
        val fuelCost = records.filter { !it.isCharge }.fold(0.0) { a, r -> a + r.cost }
        val otherCost = expenses.fold(0.0) { a, e -> a + e.amount }
        val total = chargeCost + fuelCost + otherCost

        val odoTotal = counterDelta(records.map { it.odoTotal })
        val perKm = if (odoTotal > 0L) total / odoTotal else 0.0

        /*
         * 月均按「数据实际跨度」摊薄，而不是固定除以 6。
         * 固定分母在用车首月会把月均严重压低，误导用户对养车成本的判断。
         */
        val earliest = earliestDate(records, expenses)
        val span = if (earliest == null) {
            1L
        } else {
            val between = ChronoUnit.MONTHS.between(earliest, today)
            if (between + 1L < 1L) 1L else between + 1L
        }
        val monthAvg = if (total > 0.0) total / span.toDouble() else 0.0

        return CostSummary(
            chargeCost = chargeCost,
            fuelCost = fuelCost,
            otherCost = otherCost,
            totalCost = total,
            odoTotal = odoTotal,
            costPerKm = perKm,
            monthAvg = monthAvg,
            months = monthlyCost(records, expenses, today),
            byType = costByType(expenses)
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

    /** 近 MONTHS 个月的月度费用，按时间升序（最左为最早），供柱状图直接消费 */
    private fun monthlyCost(
        records: List<Record>,
        expenses: List<Expense>,
        today: LocalDate
    ): List<MonthCost> {
        val out = ArrayList<MonthCost>(MONTHS)
        for (i in MONTHS - 1 downTo 0) {
            val target = today.minusMonths(i.toLong())
            val y = target.year
            val m = target.monthValue

            var fuel = 0.0
            for (r in records) {
                val d = ReminderCalc.parse(r.date) ?: continue
                if (d.year == y && d.monthValue == m) fuel += r.cost
            }

            var other = 0.0
            for (e in expenses) {
                val d = ReminderCalc.parse(e.date) ?: continue
                if (d.year == y && d.monthValue == m) other += e.amount
            }

            out.add(MonthCost(label = m.toString() + "月", fuel = fuel, other = other))
        }
        return out
    }

    /** 费用构成：按类型归并后降序排列，零金额类型不进入列表 */
    private fun costByType(expenses: List<Expense>): List<TypeCost> {
        val map = LinkedHashMap<String, Double>()
        for (e in expenses) {
            val prev = map[e.type] ?: 0.0
            map[e.type] = prev + e.amount
        }

        val list = ArrayList<TypeCost>()
        for ((k, v) in map) {
            if (v > 0.0) list.add(TypeCost(k, v))
        }
        // 降序：金额最大的类型排在最前，用户一眼看到主要开支
        return list.sortedByDescending { it.amount }
    }

    /** 全部记录与费用中最早的日期，用于计算月均摊薄跨度 */
    private fun earliestDate(records: List<Record>, expenses: List<Expense>): LocalDate? {
        val dates = ArrayList<LocalDate>()
        for (r in records) {
            val d = ReminderCalc.parse(r.date)
            if (d != null) dates.add(d)
        }
        for (e in expenses) {
            val d = ReminderCalc.parse(e.date)
            if (d != null) dates.add(d)
        }
        if (dates.isEmpty()) return null
        return dates.minOrNull()
    }
}
