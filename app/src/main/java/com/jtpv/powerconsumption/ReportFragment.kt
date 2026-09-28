package com.jtpv.powerconsumption

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jtpv.powerconsumption.data.CostSummary
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.TypeCost
import com.jtpv.powerconsumption.databinding.FragmentReportBinding
import java.util.Locale

/** 报表页：成本指标 + 近 6 个月趋势 + 费用构成 */
class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: Repository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(DbHelper(requireContext().applicationContext))
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refresh() {
        val placeholder = getString(R.string.value_placeholder)

        // 报表跟随全局当前车辆，不单独提供切换入口，避免与其它页的选择互相打断
        val vehicles = repo.listVehicles()
        val saved = repo.getPref(PrefKeys.CURRENT_VEHICLE).toLongOrNull()
        val veh = vehicles.firstOrNull { it.id == saved } ?: vehicles.firstOrNull()

        if (veh == null) {
            binding.tvReportVehicle.text = placeholder
            binding.chartMonthly.submit(emptyList())
            binding.tvTotalCost.text = placeholder
            binding.tvMonthAvg.text = placeholder
            binding.tvCostPerKm.text = placeholder
            binding.tvOdoTotal.text = placeholder
            binding.tvChargeCost.text = placeholder
            binding.tvFuelCost.text = placeholder
            binding.llBreakdown.removeAllViews()
            binding.tvBreakdownEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvReportVehicle.text =
            if (veh.plateNo.isBlank()) veh.name else veh.name + " · " + veh.plateNo

        val cost = repo.costSummary(veh.id)

        binding.tvTotalCost.text = String.format(Locale.US, "%.0f", cost.totalCost)
        binding.tvMonthAvg.text = String.format(Locale.US, "%.0f", cost.monthAvg)
        binding.tvCostPerKm.text =
            if (cost.costPerKm > 0.0) String.format(Locale.US, "%.2f", cost.costPerKm)
            else placeholder
        binding.tvOdoTotal.text =
            if (cost.odoTotal > 0L) cost.odoTotal.toString() else placeholder

        binding.tvChargeCost.text = String.format(Locale.US, "¥%.0f", cost.chargeCost)
        binding.tvFuelCost.text = String.format(Locale.US, "¥%.0f", cost.fuelCost)

        binding.chartMonthly.submit(cost.months)
        renderBreakdown(cost)
    }

    private fun renderBreakdown(cost: CostSummary) {
        binding.llBreakdown.removeAllViews()
        if (cost.byType.isEmpty()) {
            binding.tvBreakdownEmpty.visibility = View.VISIBLE
            return
        }
        binding.tvBreakdownEmpty.visibility = View.GONE

        // 占比条以「最大项」为满格基准：比以总额为基准更容易看出各项的相对关系
        val max = cost.byType.first().amount
        for (item in cost.byType) {
            binding.llBreakdown.addView(buildBreakdownRow(item, max))
        }
    }

    private fun buildBreakdownRow(item: TypeCost, max: Double): View {
        val ctx = requireContext()

        val box = LinearLayout(ctx)
        box.orientation = LinearLayout.VERTICAL
        val boxLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        boxLp.bottomMargin = UiUtil.dp(ctx, 10)
        box.layoutParams = boxLp

        // ---- 名称 + 金额 ----
        val top = LinearLayout(ctx)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL

        val name = TextView(ctx)
        name.text = UiUtil.expenseTypeName(ctx, item.type)
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        name.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        top.addView(
            name,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val amount = TextView(ctx)
        amount.text = String.format(Locale.US, "¥%.2f", item.amount)
        amount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        amount.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        top.addView(
            amount,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        box.addView(
            top,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // ---- 占比条：用权重表达比例，省去手工像素换算 ----
        val barBox = LinearLayout(ctx)
        barBox.orientation = LinearLayout.HORIZONTAL
        val barLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, UiUtil.dp(ctx, 6)
        )
        barLp.topMargin = UiUtil.dp(ctx, 4)

        var ratio = if (max > 0.0) (item.amount / max).toFloat() else 0f
        if (ratio < 0.02f) ratio = 0.02f   // 极小项也保留可见宽度，否则等于没画
        if (ratio > 1f) ratio = 1f

        val filled = View(ctx)
        filled.setBackgroundColor(UiUtil.expenseTypeColor(ctx, item.type))
        barBox.addView(
            filled,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, ratio)
        )

        val rest = View(ctx)
        rest.setBackgroundColor(ContextCompat.getColor(ctx, R.color.divider))
        barBox.addView(
            rest,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - ratio)
        )

        box.addView(barBox, barLp)
        return box
    }
}
