package com.jtpv.powerconsumption

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.ReminderCalc
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import com.jtpv.powerconsumption.databinding.ActivityVehicleProfileBinding

/**
 * 车辆档案与提醒设置。
 *
 * 与「补能参数」（SettingsActivity）分开：那边是影响电耗/油耗换算的物理参数
 * （电池容量、油箱容积），这边是提醒所需的档案信息，改动频率与语义都不同。
 */
class VehicleProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVehicleProfileBinding
    private lateinit var repo: Repository

    private var vehicle: Vehicle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(DbHelper(applicationContext))
        title = getString(R.string.title_vehicle_profile)

        val vehicles = repo.listVehicles()
        val saved = repo.getPref(PrefKeys.CURRENT_VEHICLE).toLongOrNull()
        vehicle = vehicles.firstOrNull { it.id == saved } ?: vehicles.firstOrNull()

        fill()
        binding.btnSaveProfile.setOnClickListener { save() }
    }

    private fun fill() {
        val v = vehicle ?: return
        binding.etPlate.setText(v.plateNo)
        binding.etModel.setText(v.modelName)
        binding.etPurchaseDate.setText(v.purchaseDate)
        binding.etOdoCurrent.setText(if (v.odoCurrent > 0L) v.odoCurrent.toString() else "")
        binding.etInsuranceExpiry.setText(v.insuranceExpiry)
        binding.etInspectionExpiry.setText(v.inspectionExpiry)
        binding.etLicenseExpiry.setText(repo.getPref(PrefKeys.LICENSE_EXPIRY))
        binding.etMaintKm.setText(if (v.maintIntervalKm > 0L) v.maintIntervalKm.toString() else "")
        binding.etMaintMonth.setText(
            if (v.maintIntervalMonth > 0) v.maintIntervalMonth.toString() else ""
        )
    }

    private fun save() {
        val v = vehicle
        if (v == null) {
            toast(getString(R.string.msg_invalid_date))
            return
        }

        val purchase = binding.etPurchaseDate.text.toString().trim()
        val insurance = binding.etInsuranceExpiry.text.toString().trim()
        val inspection = binding.etInspectionExpiry.text.toString().trim()
        val license = binding.etLicenseExpiry.text.toString().trim()

        // 日期允许留空（表示不提醒）；一旦填写就必须是合法日期，
        // 否则提醒会静默失效，而用户以为自己已经设置好了
        for (d in listOf(purchase, insurance, inspection, license)) {
            if (d.isNotEmpty() && ReminderCalc.parse(d) == null) {
                toast(getString(R.string.msg_invalid_date))
                return
            }
        }

        v.plateNo = binding.etPlate.text.toString().trim()
        v.modelName = binding.etModel.text.toString().trim()
        v.purchaseDate = purchase
        v.odoCurrent = binding.etOdoCurrent.text.toString().trim().toLongOrNull() ?: 0L
        v.insuranceExpiry = insurance
        v.inspectionExpiry = inspection
        // 留空即 0，语义为「不按该项提醒」
        v.maintIntervalKm = binding.etMaintKm.text.toString().trim().toLongOrNull() ?: 0L
        v.maintIntervalMonth = binding.etMaintMonth.text.toString().trim().toIntOrNull() ?: 0

        repo.updateVehicle(v)
        // 驾照属于驾驶人，不入 vehicle 表
        repo.setPref(PrefKeys.LICENSE_EXPIRY, license)

        toast(getString(R.string.msg_profile_saved))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
