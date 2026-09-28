package com.jtpv.powerconsumption

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import com.jtpv.powerconsumption.databinding.FragmentRecordBinding
import java.util.Locale

/** 补能页：能耗统计 + 补能记录列表 */
class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: Repository
    private lateinit var adapter: RecordAdapter

    private var vehicles: List<Vehicle> = emptyList()
    private var vehicleId: Long = 0L
    private var suppressSpinner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(DbHelper(requireContext().applicationContext))

        adapter = RecordAdapter(
            onClick = { record -> openEditor(record.id) },
            onLongClick = { record -> confirmDelete(record.id) }
        )
        binding.recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecords.adapter = adapter
        binding.fabAdd.setOnClickListener { openEditor(0L) }

        binding.spinnerVehicle.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, v: View?, position: Int, id: Long
                ) {
                    if (suppressSpinner) return
                    val veh = vehicles.getOrNull(position) ?: return
                    if (veh.id != vehicleId) {
                        vehicleId = veh.id
                        repo.setPref(PrefKeys.CURRENT_VEHICLE, veh.id.toString())
                        refresh()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    override fun onResume() {
        super.onResume()
        loadVehicles()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadVehicles() {
        vehicles = repo.listVehicles()
        if (vehicles.isEmpty()) {
            vehicleId = 0L
            binding.spinnerVehicle.adapter = null
            return
        }

        val saved = repo.getPref(PrefKeys.CURRENT_VEHICLE).toLongOrNull()
        if (saved != null && vehicles.any { it.id == saved }) {
            vehicleId = saved
        } else if (vehicleId == 0L || vehicles.none { it.id == vehicleId }) {
            vehicleId = vehicles[0].id
        }

        suppressSpinner = true
        val names = vehicles.map { it.name }
        val spinnerAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, names
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVehicle.adapter = spinnerAdapter
        val idx = vehicles.indexOfFirst { it.id == vehicleId }
        if (idx >= 0) binding.spinnerVehicle.setSelection(idx)
        suppressSpinner = false
    }

    private fun refresh() {
        val placeholder = getString(R.string.value_placeholder)

        if (vehicleId == 0L) {
            binding.tvAvgEv.text = placeholder
            binding.tvAvgFuel.text = placeholder
            binding.tvSumCharge.text = placeholder
            binding.tvSumFuel.text = placeholder
            adapter.submit(emptyList())
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        val records = repo.listRecords(vehicleId)
        adapter.submit(records)
        binding.tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE

        val stats = repo.stats(vehicleId)
        binding.tvAvgEv.text =
            if (stats.avgEv > 0.0) String.format(Locale.US, "%.2f", stats.avgEv) else placeholder
        binding.tvAvgFuel.text =
            if (stats.avgFuel > 0.0) String.format(Locale.US, "%.2f", stats.avgFuel) else placeholder
        binding.tvSumCharge.text = String.format(Locale.US, "%.1f", stats.sumCharge)
        binding.tvSumFuel.text = String.format(Locale.US, "%.1f", stats.sumFuel)
    }

    /** recordId 为 0 时表示新增 */
    private fun openEditor(recordId: Long) {
        val intent = Intent(requireContext(), RecordEditActivity::class.java)
        intent.putExtra(RecordEditActivity.EXTRA_RECORD_ID, recordId)
        intent.putExtra(RecordEditActivity.EXTRA_VEHICLE_ID, vehicleId)
        startActivity(intent)
    }

    /** 长按删除：补能记录参与电耗 / 油耗统计，误删会破坏口径，故必须二次确认 */
    private fun confirmDelete(recordId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(R.string.msg_confirm_delete_record)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repo.deleteRecord(recordId)
                Toast.makeText(
                    requireContext(), R.string.msg_deleted, Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
            // 取消键直接用系统文案，避免资源冗余
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
