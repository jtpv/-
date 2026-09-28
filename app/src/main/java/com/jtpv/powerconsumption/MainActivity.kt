package com.jtpv.powerconsumption

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import com.jtpv.powerconsumption.databinding.ActivityMainBinding
import java.util.Locale

/** 主界面：统计概览 + 记录列表 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: Repository
    private lateinit var adapter: RecordAdapter

    private var vehicles: List<Vehicle> = emptyList()
    private var currentVehicleId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = Repository(DbHelper(applicationContext))

        adapter = RecordAdapter { record -> openEditor(record.id) }
        binding.recyclerRecords.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecords.adapter = adapter

        binding.fabAdd.setOnClickListener { openEditor(0L) }

        binding.spinnerVehicle.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val v = vehicles.getOrNull(position) ?: return
                    if (v.id != currentVehicleId) {
                        currentVehicleId = v.id
                        refresh()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    override fun onResume() {
        super.onResume()
        // 从编辑页返回时需要重新加载：车辆配置可能已改、记录可能已增删
        loadVehicles()
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadVehicles() {
        vehicles = repo.listVehicles()
        if (currentVehicleId == 0L && vehicles.isNotEmpty()) {
            currentVehicleId = vehicles[0].id
        }

        val names = vehicles.map { it.name }
        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVehicle.adapter = spinnerAdapter

        val idx = vehicles.indexOfFirst { it.id == currentVehicleId }
        if (idx >= 0) binding.spinnerVehicle.setSelection(idx)
    }

    private fun refresh() {
        if (currentVehicleId == 0L) {
            adapter.submit(emptyList())
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        val records = repo.listRecords(currentVehicleId)
        adapter.submit(records)
        binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        val stats = repo.stats(currentVehicleId)
        val placeholder = getString(R.string.value_placeholder)

        binding.tvAvgEv.text =
            if (stats.avgEv > 0.0) String.format(Locale.US, "%.2f", stats.avgEv) else placeholder
        binding.tvAvgFuel.text =
            if (stats.avgFuel > 0.0) String.format(Locale.US, "%.2f", stats.avgFuel) else placeholder
        binding.tvSumCharge.text = String.format(Locale.US, "%.1f", stats.sumCharge)
        binding.tvSumFuel.text = String.format(Locale.US, "%.1f", stats.sumFuel)
    }

    /** recordId 为 0 时表示新增 */
    private fun openEditor(recordId: Long) {
        val intent = Intent(this, RecordEditActivity::class.java)
        intent.putExtra(RecordEditActivity.EXTRA_RECORD_ID, recordId)
        intent.putExtra(RecordEditActivity.EXTRA_VEHICLE_ID, currentVehicleId)
        startActivity(intent)
    }
}
