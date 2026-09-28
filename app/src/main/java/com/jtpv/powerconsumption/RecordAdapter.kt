package com.jtpv.powerconsumption

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jtpv.powerconsumption.data.Record
import com.jtpv.powerconsumption.databinding.ItemRecordBinding
import java.util.Locale

/**
 * 记录列表适配器。
 *
 * 交互约定：单击进入编辑，长按弹出删除确认——
 * 删除属破坏性操作，必须经对话框二次确认，避免误触丢数据。
 */
class RecordAdapter(
    private val onClick: (Record) -> Unit,
    private val onLongClick: (Record) -> Unit
) :
    RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    private val items = ArrayList<Record>()

    fun submit(list: List<Record>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RecordViewHolder(private val binding: ItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(r: Record) {
            val ctx = binding.root.context

            // 类型徽标：充电用绿色，加油用橙色
            if (r.isCharge) {
                binding.tvType.text = ctx.getString(R.string.type_charge)
                binding.tvType.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.ev_color)
                )
            } else {
                binding.tvType.text = ctx.getString(R.string.type_fuel)
                binding.tvType.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.fuel_color)
                )
            }

            val unit = if (r.isCharge) {
                ctx.getString(R.string.unit_kwh)
            } else {
                ctx.getString(R.string.unit_liter)
            }
            binding.tvAmount.text = String.format(Locale.US, "%.2f %s", r.amount, unit)

            binding.tvDate.text = r.date

            binding.tvOdo.text = if (r.isCharge) {
                String.format(Locale.US, "总里程 %d km · EV %d km", r.odoTotal, r.odoEv)
            } else {
                String.format(Locale.US, "总里程 %d km · HEV %d km", r.odoTotal, r.odoHev)
            }

            if (r.note.isBlank()) {
                binding.tvNote.visibility = View.GONE
            } else {
                binding.tvNote.visibility = View.VISIBLE
                binding.tvNote.text = r.note
            }

            binding.tvCost.text = if (r.cost > 0.0) {
                String.format(Locale.US, "¥%.2f", r.cost)
            } else {
                ctx.getString(R.string.value_placeholder)
            }

            binding.root.setOnClickListener { onClick(r) }
            // 返回 true 表示长按事件已消费，不再触发单击
            binding.root.setOnLongClickListener { onLongClick(r); true }
        }
    }
}
