package com.jtpv.powerconsumption

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.Record
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.databinding.ActivityRecordEditBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 记录录入 / 编辑页 */
class RecordEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordEditBinding
    private lateinit var repo: Repository

    private var recordId: Long = 0L
    private var vehicleId: Long = 1L
    private var editing: Record? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(DbHelper(applicationContext))
        recordId = intent.getLongExtra(EXTRA_RECORD_ID, 0L)
        vehicleId = intent.getLongExtra(EXTRA_VEHICLE_ID, 1L)

        editing = if (recordId > 0L) repo.getRecord(recordId) else null
        title = getString(
            if (editing != null) R.string.title_edit_record else R.string.title_add_record
        )

        binding.rgType.setOnCheckedChangeListener { _, _ -> applyTypeUi() }
        binding.btnSave.setOnClickListener { save() }

        fillForm()
    }

    private fun isCharge(): Boolean = binding.rbCharge.isChecked

    /** 按记录类型切换提示文字与字段可见性 */
    private fun applyTypeUi() {
        val charge = isCharge()

        binding.etAmount.hint = getString(
            if (charge) R.string.hint_amount_charge else R.string.hint_amount_fuel
        )
        binding.swFull.text = getString(
            if (charge) R.string.label_is_full_charge else R.string.label_is_full_fuel
        )

        // 充电记录关注 EV 里程，加油记录关注 HEV 里程
        binding.etOdoEv.visibility = if (charge) View.VISIBLE else View.GONE
        binding.etOdoHev.visibility = if (charge) View.GONE else View.VISIBLE
    }

    private fun fillForm() {
        val r = editing
        if (r == null) {
            binding.rbCharge.isChecked = true
            binding.etDate.setText(today())
            applyTypeUi()
            return
        }

        if (r.isCharge) {
            binding.rbCharge.isChecked = true
        } else {
            binding.rbFuel.isChecked = true
        }
        binding.etDate.setText(r.date)
        binding.etAmount.setText(fmt(r.amount))
        binding.etPrice.setText(fmt(r.price))
        binding.etCost.setText(fmt(r.cost))
        binding.etOdoTotal.setText(fmtLong(r.odoTotal))
        binding.etOdoEv.setText(fmtLong(r.odoEv))
        binding.etOdoHev.setText(fmtLong(r.odoHev))
        binding.swFull.isChecked = r.isFull
        binding.etNote.setText(r.note)

        applyTypeUi()
    }

    private fun save() {
        val date = binding.etDate.text.toString().trim()
        if (!DATE_PATTERN.matches(date)) {
            toast(getString(R.string.msg_invalid_date))
            return
        }

        val amount = binding.etAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            toast(getString(R.string.msg_invalid_amount))
            return
        }

        val price = binding.etPrice.text.toString().trim().toDoubleOrNull() ?: 0.0
        var cost = binding.etCost.text.toString().trim().toDoubleOrNull() ?: 0.0
        if (cost <= 0.0 && price > 0.0) {
            // 未填金额时按 单价 × 数量 估算
            cost = price * amount
        }

        val charge = isCharge()
        val rec = editing ?: Record()

        rec.vehicleId = vehicleId
        rec.type = if (charge) Record.TYPE_CHARGE else Record.TYPE_FUEL
        rec.date = date
        rec.amount = amount
        rec.price = price
        rec.cost = cost
        rec.odoTotal = binding.etOdoTotal.text.toString().trim().toLongOrNull() ?: 0L
        rec.odoEv = if (charge) {
            binding.etOdoEv.text.toString().trim().toLongOrNull() ?: 0L
        } else {
            0L
        }
        rec.odoHev = if (charge) {
            0L
        } else {
            binding.etOdoHev.text.toString().trim().toLongOrNull() ?: 0L
        }
        rec.isFull = binding.swFull.isChecked
        rec.note = binding.etNote.text.toString().trim()

        if (rec.id > 0L) {
            repo.updateRecord(rec)
        } else {
            repo.insertRecord(rec)
        }

        toast(getString(R.string.msg_saved))
        finish()
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun fmt(v: Double): String =
        if (v > 0.0) String.format(Locale.US, "%.2f", v) else ""

    private fun fmtLong(v: Long): String = if (v > 0L) v.toString() else ""

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"

        private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
