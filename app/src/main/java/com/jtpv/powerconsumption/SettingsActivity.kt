package com.jtpv.powerconsumption

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import com.jtpv.powerconsumption.data.VehiclePresets
import com.jtpv.powerconsumption.databinding.ActivitySettingsBinding
import java.util.Locale

/**
 * 车辆设置页。
 * 电池容量与油箱容积必须可配置——不同年款差异很大，硬编码必然出错。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repo: Repository

    private var vehicle: Vehicle? = null

    /** 程序化设置 Spinner 会触发回调，用此标志避免覆盖用户手填的数值 */
    private var suppressPresetCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(DbHelper(applicationContext))
        title = getString(R.string.title_settings)

        vehicle = repo.listVehicles().firstOrNull()

        val names = resources.getStringArray(R.array.preset_names)
        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names.toList()
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPreset.adapter = spinnerAdapter

        binding.spinnerPreset.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    if (suppressPresetCallback) return
                    if (VehiclePresets.isCustom(position)) return
                    if (position >= VehiclePresets.batteryKwh.size) return

                    // 选中预设年款时自动带出电池与油箱参数
                    binding.etBattery.setText(
                        String.format(Locale.US, "%.2f", VehiclePresets.batteryKwh[position])
                    )
                    binding.etTank.setText(
                        String.format(Locale.US, "%.0f", VehiclePresets.tankLiter[position])
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        fill()
        binding.btnSaveSettings.setOnClickListener { save() }
    }

    private fun fill() {
        val v = vehicle
        val battery = v?.batteryKwh ?: 0.0
        val tank = v?.tankLiter ?: 0.0

        binding.etVehicleName.setText(v?.name ?: "")
        binding.etBattery.setText(
            if (battery > 0.0) String.format(Locale.US, "%.2f", battery) else ""
        )
        binding.etTank.setText(
            if (tank > 0.0) String.format(Locale.US, "%.0f", tank) else ""
        )

        val index = (v?.presetIndex ?: Vehicle.PRESET_CUSTOM)
            .coerceIn(0, VehiclePresets.batteryKwh.size - 1)

        suppressPresetCallback = true
        binding.spinnerPreset.setSelection(index)
        suppressPresetCallback = false
    }

    private fun save() {
        val name = binding.etVehicleName.text.toString().trim().ifBlank { "我的车" }
        val battery = binding.etBattery.text.toString().trim().toDoubleOrNull() ?: 0.0
        val tank = binding.etTank.text.toString().trim().toDoubleOrNull() ?: 0.0
        val preset = binding.spinnerPreset.selectedItemPosition

        val v = vehicle
        if (v == null) {
            repo.insertVehicle(
                Vehicle(name = name, batteryKwh = battery, tankLiter = tank, presetIndex = preset)
            )
        } else {
            v.name = name
            v.batteryKwh = battery
            v.tankLiter = tank
            v.presetIndex = preset
            repo.updateVehicle(v)
        }

        Toast.makeText(this, getString(R.string.msg_settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}
