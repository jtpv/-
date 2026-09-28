package com.jtpv.powerconsumption

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.Expense
import com.jtpv.powerconsumption.data.ExpenseType
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import com.jtpv.powerconsumption.databinding.ActivityReminderBinding

/**
 * 保养与提醒页。
 *
 * 保养记录与费用记录共用 expense 表（type = MAINTENANCE），此处只是按类型筛选的
 * 专用视图 —— 若再建一张保养表，同一笔支出就有两个入口，迟早出现重复计入。
 */
class ReminderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderBinding
    private lateinit var repo: Repository
    private lateinit var adapter: ExpenseAdapter

    private var vehicleId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(DbHelper(applicationContext))
        title = getString(R.string.title_reminder)

        adapter = ExpenseAdapter(
            onClick = { expense -> openEditor(expense.id) },
            onLongClick = { expense -> confirmDelete(expense.id) }
        )
        binding.recyclerMaint.layoutManager = LinearLayoutManager(this)
        binding.recyclerMaint.adapter = adapter
        binding.fabAddMaint.setOnClickListener { openEditor(0L) }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val vehicles = repo.listVehicles()
        val saved = repo.getPref(PrefKeys.CURRENT_VEHICLE).toLongOrNull()
        val veh: Vehicle? = vehicles.firstOrNull { it.id == saved } ?: vehicles.firstOrNull()

        if (veh == null) {
            vehicleId = 0L
            binding.llReminders.removeAllViews()
            binding.tvNoReminder.visibility = View.VISIBLE
            binding.tvMaintSummary.text = getString(R.string.home_maint_unset)
            adapter.submit(emptyList())
            binding.recyclerMaint.visibility = View.GONE
            binding.tvMaintEmpty.visibility = View.VISIBLE
            return
        }

        vehicleId = veh.id

        // 详情页列出未设置项，作为「去档案页补全」的引导
        val reminders = ReminderBuilder.build(this, repo, veh, true)
        binding.llReminders.removeAllViews()
        if (reminders.isEmpty()) {
            binding.tvNoReminder.visibility = View.VISIBLE
        } else {
            binding.tvNoReminder.visibility = View.GONE
            for (item in reminders) {
                binding.llReminders.addView(UiUtil.buildReminderRow(this, item))
            }
        }

        binding.tvMaintSummary.text = ReminderBuilder.maintenanceSummary(this, repo, veh)

        val list = repo.listExpenses(vehicleId, ExpenseType.MAINTENANCE)
        adapter.submit(list)
        binding.recyclerMaint.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        binding.tvMaintEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(expenseId: Long) {
        val intent = Intent(this, ExpenseEditActivity::class.java)
        intent.putExtra(ExpenseEditActivity.EXTRA_EXPENSE_ID, expenseId)
        intent.putExtra(ExpenseEditActivity.EXTRA_VEHICLE_ID, vehicleId)
        intent.putExtra(ExpenseEditActivity.EXTRA_PRESET_TYPE, ExpenseType.MAINTENANCE)
        startActivity(intent)
    }

    /** 长按删除：保养记录本质是 expense 行，删除后须同时刷新提醒计算 */
    private fun confirmDelete(expenseId: Long) {
        AlertDialog.Builder(this)
            .setTitle(R.string.action_delete)
            .setMessage(R.string.msg_confirm_delete_expense)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repo.deleteExpense(expenseId)
                Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show()
                load()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
