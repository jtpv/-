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
import com.jtpv.powerconsumption.databinding.FragmentHomeBinding
import java.util.Locale

/** 首页：到期提醒 + 关键指标 + 保养概览 + 最近补能 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    /** Fragment 的视图比 Fragment 本身活得短，必须置空，否则旋转后必然泄漏 */
    private val binding get() = _binding!!

    private lateinit var repo: Repository
    private lateinit var adapter: RecordAdapter

    private var vehicles: List<Vehicle> = emptyList()
    private var vehicleId: Long = 0L

    /** 程序化回填 Spinner 也会触发回调，用标志避免把「回填」误当成用户切换 */
    private var suppressSpinner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 用 applicationContext 建库：Fragment 随配置变更频繁重建，持有 Activity 引用易泄漏
        repo = Repository(DbHelper(requireContext().applicationContext))

        adapter = RecordAdapter(
            onClick = { record -> openEditor(record.id) },
            onLongClick = { record -> confirmDelete(record.id) }
        )
        binding.recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecent.adapter = adapter

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
        // 从子页返回后车辆档案与记录都可能已变，必须重新加载
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

        // 当前车辆跨页面共享，优先沿用偏好中记录的那一辆
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
        val veh = vehicles.firstOrNull { it.id == vehicleId }

        if (veh == null) {
            binding.tvAvgEv.text = placeholder
            binding.tvAvgFuel.text = placeholder
            binding.tvSumCost.text = placeholder
            binding.tvCostPerKm.text = placeholder
            binding.llReminders.removeAllViews()
            binding.tvNoReminder.visibility = View.VISIBLE
            binding.tvMaintInfo.text = getString(R.string.home_maint_unset)
            adapter.submit(emptyList())
            binding.recyclerRecent.visibility = View.GONE
            binding.tvRecentEmpty.visibility = View.VISIBLE
            return
        }

        // ===== 关键指标 =====
        val stats = repo.stats(vehicleId)
        val cost = repo.costSummary(vehicleId)

        binding.tvAvgEv.text =
            if (stats.avgEv > 0.0) String.format(Locale.US, "%.2f", stats.avgEv) else placeholder
        binding.tvAvgFuel.text =
            if (stats.avgFuel > 0.0) String.format(Locale.US, "%.2f", stats.avgFuel) else placeholder
        binding.tvSumCost.text = String.format(Locale.US, "%.0f", cost.totalCost)
        binding.tvCostPerKm.text =
            if (cost.costPerKm > 0.0) String.format(Locale.US, "%.2f", cost.costPerKm)
            else placeholder

        // ===== 到期提醒：首页只列需要关注的项，未设置的留给详情页引导 =====
        val reminders = ReminderBuilder.build(requireContext(), repo, veh, false)
        binding.llReminders.removeAllViews()
        if (reminders.isEmpty()) {
            binding.tvNoReminder.visibility = View.VISIBLE
        } else {
            binding.tvNoReminder.visibility = View.GONE
            for (item in reminders) {
                binding.llReminders.addView(UiUtil.buildReminderRow(requireContext(), item))
            }
        }

        // ===== 保养概览 =====
        binding.tvMaintInfo.text = ReminderBuilder.maintenanceSummary(requireContext(), repo, veh)

        // ===== 最近补能（只展示最近 3 条，全量列表在补能页） =====
        val recent = repo.listRecords(vehicleId).take(3)
        adapter.submit(recent)
        binding.recyclerRecent.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        binding.tvRecentEmpty.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
    }

    /** recordId 为 0 时表示新增 */
    private fun openEditor(recordId: Long) {
        val intent = Intent(requireContext(), RecordEditActivity::class.java)
        intent.putExtra(RecordEditActivity.EXTRA_RECORD_ID, recordId)
        intent.putExtra(RecordEditActivity.EXTRA_VEHICLE_ID, vehicleId)
        startActivity(intent)
    }

    /** 长按删除：与补能页保持一致的交互，删除需二次确认 */
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
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
