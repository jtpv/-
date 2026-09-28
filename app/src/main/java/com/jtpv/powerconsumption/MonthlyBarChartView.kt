package com.jtpv.powerconsumption

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.jtpv.powerconsumption.data.MonthCost
import java.util.Locale

/**
 * 近 N 个月费用柱状图。
 *
 * 自绘而非引入图表库：本项目在无法本地编译的环境下构建，每增加一个第三方依赖
 * 都是一次云端拉取失败的机会；而这里只需要「分组柱 + 月份标签」，自绘成本极低。
 *
 * 柱子自下而上分为两段：下方为补能费用（橙），上方为其他费用（蓝），
 * 便于一眼看出某月的高支出是加油多还是保养等杂项多。
 */
class MonthlyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val items = ArrayList<MonthCost>()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val fuelColor = ContextCompat.getColor(context, R.color.chart_fuel)
    private val otherColor = ContextCompat.getColor(context, R.color.chart_other)
    private val gridColor = ContextCompat.getColor(context, R.color.chart_grid)
    private val textColor = ContextCompat.getColor(context, R.color.chart_text)
    private val primaryColor = ContextCompat.getColor(context, R.color.text_primary)

    init {
        labelPaint.color = textColor
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.textSize = sp(10f)

        gridPaint.color = gridColor
        gridPaint.strokeWidth = dp(1f)
    }

    fun submit(list: List<MonthCost>) {
        items.clear()
        items.addAll(list)
        invalidate()
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
        )

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics
        )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 顶部留出峰值标注的高度，底部留出月份标签的高度
        val left = paddingLeft + dp(8f)
        val right = width - paddingRight - dp(8f)
        val top = paddingTop + sp(15f)
        val bottom = height - paddingBottom - sp(17f)

        val plotWidth = right - left
        val plotHeight = bottom - top
        if (plotWidth <= 0f || plotHeight <= 0f) return

        canvas.drawLine(left, bottom, right, bottom, gridPaint)

        var max = 0.0
        for (m in items) {
            if (m.total > max) max = m.total
        }
        if (items.isEmpty() || max <= 0.0) {
            labelPaint.color = textColor
            canvas.drawText(
                context.getString(R.string.report_no_data),
                (left + right) / 2f,
                (top + bottom) / 2f,
                labelPaint
            )
            return
        }

        val slot = plotWidth / items.size
        val barWidth = slot * 0.46f

        for (i in items.indices) {
            val m = items[i]
            val cx = left + slot * i + slot / 2f

            val totalH = (plotHeight * (m.total / max)).toFloat()
            val fuelH = if (m.total > 0.0) (totalH * (m.fuel / m.total)).toFloat() else 0f
            val otherH = totalH - fuelH

            val barLeft = cx - barWidth / 2f
            val barRight = cx + barWidth / 2f

            // 其他费用在上、补能费用在下，两段相接构成整柱
            if (otherH > 0f) {
                barPaint.color = otherColor
                canvas.drawRect(barLeft, bottom - totalH, barRight, bottom - fuelH, barPaint)
            }
            if (fuelH > 0f) {
                barPaint.color = fuelColor
                canvas.drawRect(barLeft, bottom - fuelH, barRight, bottom, barPaint)
            }

            canvas.drawText(m.label, cx, bottom + sp(13f), labelPaint)
        }

        // 峰值标注：置于图顶部居中，避免压住最高的柱子
        labelPaint.color = primaryColor
        canvas.drawText(
            String.format(Locale.US, "%.0f", max),
            left + plotWidth / 2f,
            paddingTop + sp(12f),
            labelPaint
        )
    }
}
