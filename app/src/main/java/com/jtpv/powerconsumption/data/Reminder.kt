package com.jtpv.powerconsumption.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** 偏好设置键（存于 pref 键值表，用于跨页面共享的少量状态） */
object PrefKeys {

    /** 当前选中车辆的 id —— 底部导航各页面共享同一辆车，避免各自为政 */
    const val CURRENT_VEHICLE = "current_vehicle_id"

    /** 驾驶证到期日。属于驾驶人而非车辆，故不入 vehicle 表 */
    const val LICENSE_EXPIRY = "license_expiry"
}

/**
 * 一条到期提醒。
 *
 * label 与 detail 由调用方本地化后传入 —— 本层刻意不依赖 Android 资源，
 * 以便计算逻辑可独立测试。
 *
 * daysLeft 为 null 表示该项未设置（而非到期）。二者语义完全不同，不可合并。
 */
data class ReminderItem(
    val label: String,
    val detail: String,
    val daysLeft: Int?,
    val level: Int
) {
    companion object {
        const val LEVEL_NORMAL = 0    // 30 天以上
        const val LEVEL_WARN = 1      // 30 天内
        const val LEVEL_URGENT = 2    // 7 天内
        const val LEVEL_EXPIRED = 3   // 已过期
    }
}

/**
 * 到期提醒计算。
 *
 * 统一使用 java.time —— minSdk 已为 26，无需 desugaring。
 */
object ReminderCalc {

    private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** 预警阈值（天）：先给结果，再给依据 */
    const val WARN_DAYS = 30
    const val URGENT_DAYS = 7

    /** 解析 yyyy-MM-dd。格式非法或为空一律返回 null，不做静默兜底为「今天」 */
    fun parse(date: String): LocalDate? {
        val s = date.trim()
        if (s.isEmpty()) return null
        return try {
            LocalDate.parse(s, FMT)
        } catch (ex: Exception) {
            null
        }
    }

    /** 剩余天数，负数表示已过期；无法解析时返回 null */
    fun daysLeft(dueDate: String, today: LocalDate): Int? {
        val due = parse(dueDate) ?: return null
        return ChronoUnit.DAYS.between(today, due).toInt()
    }

    /**
     * 分级：不足 7 天为紧急，不足 30 天为预警，已过期为过期。
     * days 为 null（未设置）时按「正常」处理，由界面另行提示未设置。
     */
    fun level(days: Int?): Int {
        if (days == null) return ReminderItem.LEVEL_NORMAL
        return when {
            days < 0 -> ReminderItem.LEVEL_EXPIRED
            days < URGENT_DAYS -> ReminderItem.LEVEL_URGENT
            days < WARN_DAYS -> ReminderItem.LEVEL_WARN
            else -> ReminderItem.LEVEL_NORMAL
        }
    }

    /** 按日期加月数，用于从上次保养日推算下次保养日 */
    fun plusMonths(date: LocalDate, months: Int): LocalDate = date.plusMonths(months.toLong())

    fun format(date: LocalDate): String = date.format(FMT)

    fun today(): LocalDate = LocalDate.now()
}
