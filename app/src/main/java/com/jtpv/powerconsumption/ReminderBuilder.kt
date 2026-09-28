package com.jtpv.powerconsumption

import android.content.Context
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.ReminderCalc
import com.jtpv.powerconsumption.data.ReminderItem
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import java.time.LocalDate

/**
 * 提醒清单构建。
 *
 * 首页与「保养与提醒」页共用同一套口径 —— 两处若各自实现，迟早出现
 * 「首页说还有 3 天、详情页说已过期」这类自相矛盾的结果。
 */
object ReminderBuilder {

    /** 里程口径的预警阈值：剩余不足 1500 km 预警，不足 500 km 紧急 */
    private const val KM_WARN = 1500L
    private const val KM_URGENT = 500L

    /**
     * @param includeUnset true 时把「未设置」项也列出（详情页用于引导补全档案），
     *                     false 时只列出真正需要关注的项（首页用于减少噪音）
     */
    fun build(
        ctx: Context,
        repo: Repository,
        veh: Vehicle,
        includeUnset: Boolean
    ): List<ReminderItem> {
        val today = ReminderCalc.today()
        val out = ArrayList<ReminderItem>()

        addExpiry(
            ctx, out, ctx.getString(R.string.reminder_insurance),
            veh.insuranceExpiry, today, includeUnset
        )
        addExpiry(
            ctx, out, ctx.getString(R.string.reminder_inspection),
            veh.inspectionExpiry, today, includeUnset
        )
        addExpiry(
            ctx, out, ctx.getString(R.string.reminder_license),
            repo.getPref(PrefKeys.LICENSE_EXPIRY), today, includeUnset
        )

        val maint = maintenanceReminder(ctx, repo, veh, today, includeUnset)
        if (maint != null) out.add(maint)

        // 等级越高越紧急，排在最前，便于优先处理
        return out.sortedByDescending { it.level }
    }

    /** 保养概览文本：说明上次保养的基准与推算出的下次保养节点 */
    fun maintenanceSummary(ctx: Context, repo: Repository, veh: Vehicle): String {
        val lastOdo = repo.lastMaintenanceOdo(veh.id)
        val lastDate = repo.lastMaintenanceDate(veh.id)
        if (lastOdo <= 0L && lastDate.isBlank()) {
            return ctx.getString(R.string.home_maint_unset)
        }

        val lines = ArrayList<String>()
        if (lastDate.isNotBlank()) {
            lines.add(ctx.getString(R.string.maint_last_date, lastDate))
        }
        if (lastOdo > 0L) {
            lines.add(ctx.getString(R.string.maint_last_odo, lastOdo))
            if (veh.maintIntervalKm > 0L) {
                lines.add(
                    ctx.getString(R.string.maint_next_odo, lastOdo + veh.maintIntervalKm)
                )
            }
        }
        if (veh.maintIntervalMonth > 0) {
            val d = ReminderCalc.parse(lastDate)
            if (d != null) {
                lines.add(
                    ctx.getString(
                        R.string.maint_next_date,
                        ReminderCalc.format(ReminderCalc.plusMonths(d, veh.maintIntervalMonth))
                    )
                )
            }
        }
        return lines.joinToString("\n")
    }

    private fun addExpiry(
        ctx: Context,
        out: MutableList<ReminderItem>,
        label: String,
        dueDate: String,
        today: LocalDate,
        includeUnset: Boolean
    ) {
        val days = ReminderCalc.daysLeft(dueDate, today)
        if (days == null) {
            if (includeUnset) {
                out.add(
                    ReminderItem(
                        label,
                        ctx.getString(R.string.reminder_unset),
                        null,
                        ReminderItem.LEVEL_NORMAL
                    )
                )
            }
            return
        }
        out.add(ReminderItem(label, describe(ctx, days), days, ReminderCalc.level(days)))
    }

    /**
     * 下次保养提醒。
     *
     * 采用双口径：里程与时间。插混车型若长期市区纯电通勤，里程增长很慢，
     * 只看里程会让机油超期服役；反之常跑高速的用户里程先到。两者取更紧迫的一个定级。
     */
    private fun maintenanceReminder(
        ctx: Context,
        repo: Repository,
        veh: Vehicle,
        today: LocalDate,
        includeUnset: Boolean
    ): ReminderItem? {
        val label = ctx.getString(R.string.reminder_maintenance)
        val lastOdo = repo.lastMaintenanceOdo(veh.id)
        val lastDate = ReminderCalc.parse(repo.lastMaintenanceDate(veh.id))

        var kmLeft: Long? = null
        if (veh.maintIntervalKm > 0L && lastOdo > 0L && veh.odoCurrent > 0L) {
            kmLeft = lastOdo + veh.maintIntervalKm - veh.odoCurrent
        }

        var dateLeft: Int? = null
        var dueDateText = ""
        if (veh.maintIntervalMonth > 0 && lastDate != null) {
            val due = ReminderCalc.plusMonths(lastDate, veh.maintIntervalMonth)
            dateLeft = ReminderCalc.daysLeft(ReminderCalc.format(due), today)
            dueDateText = ReminderCalc.format(due)
        }

        if (kmLeft == null && dateLeft == null) {
            if (!includeUnset) return null
            return ReminderItem(
                label,
                ctx.getString(R.string.reminder_maint_km_unset),
                null,
                ReminderItem.LEVEL_NORMAL
            )
        }

        val lvKm = if (kmLeft == null) Int.MIN_VALUE else kmLevel(kmLeft)
        val lvDate = if (dateLeft == null) Int.MIN_VALUE else ReminderCalc.level(dateLeft)
        val level = if (lvKm >= lvDate) lvKm else lvDate

        val parts = ArrayList<String>()
        if (kmLeft != null) {
            parts.add(
                if (kmLeft < 0L) {
                    ctx.getString(R.string.reminder_maint_km_over, -kmLeft)
                } else {
                    ctx.getString(R.string.reminder_maint_km_left, kmLeft)
                }
            )
        }
        if (dateLeft != null) {
            parts.add(dueDateText + " · " + describe(ctx, dateLeft))
        }

        return ReminderItem(label, parts.joinToString("  /  "), dateLeft, level)
    }

    private fun kmLevel(kmLeft: Long): Int = when {
        kmLeft < 0L -> ReminderItem.LEVEL_EXPIRED
        kmLeft < KM_URGENT -> ReminderItem.LEVEL_URGENT
        kmLeft < KM_WARN -> ReminderItem.LEVEL_WARN
        else -> ReminderItem.LEVEL_NORMAL
    }

    /** 过期与当日需要单独措辞 —— 「剩余 0 天」读起来像还没到期 */
    private fun describe(ctx: Context, days: Int): String = when {
        days < 0 -> ctx.getString(R.string.reminder_days_overdue, -days)
        days == 0 -> ctx.getString(R.string.reminder_due_today)
        else -> ctx.getString(R.string.reminder_days_left, days)
    }
}
