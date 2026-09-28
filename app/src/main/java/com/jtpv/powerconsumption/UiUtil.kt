package com.jtpv.powerconsumption

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jtpv.powerconsumption.data.ExpenseType
import com.jtpv.powerconsumption.data.ReminderItem

/**
 * 界面公共构件。
 *
 * 提醒行采用代码构建而非「布局 + 适配器」：其结构固定为两行文本，
 * 为一个单一形态再引入布局文件与 ViewHolder 只会增加出错面。
 */
object UiUtil {

    /** 费用类型的本地化名称。索引与 ExpenseType.all 一一对应 */
    fun expenseTypeName(ctx: Context, type: String): String {
        val names = ctx.resources.getStringArray(R.array.expense_type_names)
        val idx = ExpenseType.indexOf(type)
        return if (idx in names.indices) names[idx] else type
    }

    /** 费用类型对应的徽标色 */
    fun expenseTypeColor(ctx: Context, type: String): Int {
        val res = when (type) {
            ExpenseType.MAINTENANCE -> R.color.exp_maintenance
            ExpenseType.REPAIR -> R.color.exp_repair
            ExpenseType.INSURANCE -> R.color.exp_insurance
            ExpenseType.INSPECTION -> R.color.exp_inspection
            ExpenseType.PARKING -> R.color.exp_parking
            ExpenseType.TOLL -> R.color.exp_toll
            ExpenseType.WASH -> R.color.exp_wash
            ExpenseType.FINE -> R.color.exp_fine
            ExpenseType.ACCESSORY -> R.color.exp_accessory
            else -> R.color.exp_other
        }
        return ContextCompat.getColor(ctx, res)
    }

    /** 提醒等级对应的文字色：越紧急越偏红 */
    fun levelColor(ctx: Context, level: Int): Int {
        val res = when (level) {
            ReminderItem.LEVEL_EXPIRED -> R.color.level_expired
            ReminderItem.LEVEL_URGENT -> R.color.level_urgent
            ReminderItem.LEVEL_WARN -> R.color.level_warn
            else -> R.color.level_normal
        }
        return ContextCompat.getColor(ctx, res)
    }

    fun dp(ctx: Context, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), ctx.resources.displayMetrics
        ).toInt()

    /**
     * 构建一条提醒行：标题用等级色加粗，下方一行说明剩余量。
     * 说明单独占一行而非与标题同行 —— 中文说明较长，同行会被挤成一行省略号。
     */
    fun buildReminderRow(ctx: Context, item: ReminderItem): View {
        val pad = dp(ctx, 12)

        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(pad, pad, pad, pad)
        row.setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_bg))

        val title = TextView(ctx)
        title.text = item.label
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        title.setTextColor(levelColor(ctx, item.level))
        title.setTypeface(Typeface.DEFAULT_BOLD)
        row.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val detail = TextView(ctx)
        detail.text = item.detail
        detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        detail.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        val detailLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        detailLp.topMargin = dp(ctx, 4)
        row.addView(detail, detailLp)

        // 行间留白由自身外边距提供，避免外层容器再包一层
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = dp(ctx, 6)
        row.layoutParams = lp

        return row
    }
}
