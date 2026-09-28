package com.jtpv.powerconsumption

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jtpv.powerconsumption.data.Expense
import com.jtpv.powerconsumption.databinding.ItemExpenseBinding
import java.util.Locale

/**
 * 费用列表适配器。
 *
 * 交互与补能记录保持一致：单击编辑、长按删除（删除需经确认）。
 */
class ExpenseAdapter(
    private val onClick: (Expense) -> Unit,
    private val onLongClick: (Expense) -> Unit
) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val items = ArrayList<Expense>()

    fun submit(list: List<Expense>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(e: Expense) {
            val ctx = binding.root.context

            binding.tvExpType.text = UiUtil.expenseTypeName(ctx, e.type)
            binding.tvExpType.setBackgroundColor(UiUtil.expenseTypeColor(ctx, e.type))

            binding.tvExpAmount.text = String.format(Locale.US, "¥%.2f", e.amount)
            binding.tvExpDate.text = e.date

            // 里程与商家按有无拼装，避免出现「0 km · 」这类残缺文本
            val meta = ArrayList<String>()
            if (e.odo > 0L) {
                meta.add(String.format(Locale.US, "%d km", e.odo))
            }
            if (e.shop.isNotBlank()) {
                meta.add(e.shop)
            }
            if (meta.isEmpty()) {
                binding.tvExpMeta.visibility = View.GONE
            } else {
                binding.tvExpMeta.visibility = View.VISIBLE
                binding.tvExpMeta.text = meta.joinToString(" · ")
            }

            if (e.note.isBlank()) {
                binding.tvExpNote.visibility = View.GONE
            } else {
                binding.tvExpNote.visibility = View.VISIBLE
                binding.tvExpNote.text = e.note
            }

            binding.root.setOnClickListener { onClick(e) }
            // 返回 true 表示长按事件已消费，不再触发单击
            binding.root.setOnLongClickListener { onLongClick(e); true }
        }
    }
}
