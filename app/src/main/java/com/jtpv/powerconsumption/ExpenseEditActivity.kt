package com.jtpv.powerconsumption

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.Expense
import com.jtpv.powerconsumption.data.ExpenseType
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.databinding.ActivityExpenseEditBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 费用录入 / 编辑页 */
class ExpenseEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseEditBinding
    private lateinit var repo: Repository

    private var expenseId: Long = 0L
    private var vehicleId: Long = 1L
    private var editing: Expense? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(DbHelper(applicationContext))
        expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, 0L)
        vehicleId = intent.getLongExtra(EXTRA_VEHICLE_ID, 1L)
        val presetType = intent.getStringExtra(EXTRA_PRESET_TYPE)

        editing = if (expenseId > 0L) repo.getExpense(expenseId) else null
        title = getString(
            if (editing != null) R.string.title_edit_expense else R.string.title_add_expense
        )

        val typeNames = resources.getStringArray(R.array.expense_type_names).toList()
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeNames)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = typeAdapter

        binding.btnSaveExpense.setOnClickListener { save() }
        fillForm(presetType)
    }

    private fun fillForm(presetType: String?) {
        val e = editing
        if (e == null) {
            // 新增：允许调用方指定默认类型（从保养页进来时默认「保养」）
            val idx = ExpenseType.indexOf(presetType ?: ExpenseType.MAINTENANCE)
            binding.spinnerType.setSelection(idx)
            binding.etExpDate.setText(today())
            return
        }

        binding.spinnerType.setSelection(ExpenseType.indexOf(e.type))
        binding.etExpDate.setText(e.date)
        binding.etExpAmount.setText(fmt(e.amount))
        binding.etExpOdo.setText(if (e.odo > 0L) e.odo.toString() else "")
        binding.etExpShop.setText(e.shop)
        binding.etExpNote.setText(e.note)
    }

    private fun save() {
        val date = binding.etExpDate.text.toString().trim()
        if (!DATE_PATTERN.matches(date)) {
            toast(getString(R.string.msg_invalid_date))
            return
        }

        val amount = binding.etExpAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            toast(getString(R.string.msg_invalid_expense_amount))
            return
        }

        // selectedItemPosition 理论上不会越界，但 setSelection 传入非法下标时
        // 会静默落到 0，这里显式收敛，避免类型被悄悄改写成第一项
        val pos = binding.spinnerType.selectedItemPosition.coerceIn(0, ExpenseType.all.size - 1)

        val e = editing ?: Expense()
        e.vehicleId = vehicleId
        e.type = ExpenseType.all[pos]
        e.date = date
        e.amount = amount
        e.odo = binding.etExpOdo.text.toString().trim().toLongOrNull() ?: 0L
        e.shop = binding.etExpShop.text.toString().trim()
        e.note = binding.etExpNote.text.toString().trim()

        if (e.id > 0L) {
            repo.updateExpense(e)
        } else {
            repo.insertExpense(e)
        }

        toast(getString(R.string.msg_saved))
        finish()
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun fmt(v: Double): String =
        if (v > 0.0) String.format(Locale.US, "%.2f", v) else ""

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_EXPENSE_ID = "extra_expense_id"
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"

        /** 从「保养与提醒」页进入时，用它把类型预设为保养 */
        const val EXTRA_PRESET_TYPE = "extra_preset_type"

        private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
