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
import com.jtpv.powerconsumption.data.ExpenseType
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.data.Vehicle
import com.jtpv.powerconsumption.databinding.FragmentExpenseBinding

/** 费用页：按类型筛选的费用流水与合计 */
class ExpenseFragment : Fragment() {

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: Repository
    private lateinit var adapter: ExpenseAdapter

    private var vehicles: List<Vehicle> = emptyList()
    private var vehicleId: Long = 0L

    /** null 表示「全部类型」，与「某个类型恰好没有记录」是两回事 */
    private var typeFilter: String? = null

    private var suppressSpinner = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(DbHelper(requireContext().applicationContext))

        adapter = ExpenseAdapter(
            onClick = { expense -> openEditor(expense.id) },
            onLongClick = { expense -> confirmDelete(expense.id) }
        )
        binding.recyclerExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExpenses.adapter = adapter
        binding.fabAddExpense.setOnClickListener { openEditor(0L) }

        // 筛选下拉首项为「全部」，其余按 ExpenseType.all 的顺序排列
        val typeNames = ArrayList<String>()
        typeNames.add(getString(R.string.expense_filter_all))
        typeNames.addAll(
            requireContext().resources.getStringArray(R.array.expense_type_names).toList()
        )
        val typeAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, typeNames
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTypeFilter.adapter = typeAdapter
        binding.spinnerTypeFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, v: View?, position: Int, id: Long
                ) {
                    typeFilter = if (position <= 0) null else ExpenseType.all.getOrNull(position - 1)
                    refresh()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

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
        if (vehicleId == 0L) {
            adapter.submit(emptyList())
            binding.tvExpenseSummary.text = ""
            binding.recyclerExpenses.visibility = View.GONE
            binding.tvExpenseEmpty.visibility = View.VISIBLE
            return
        }

        val list = repo.listExpenses(vehicleId, typeFilter)
        adapter.submit(list)
        binding.recyclerExpenses.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        binding.tvExpenseEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        val sum = list.fold(0.0) { acc, e -> acc + e.amount }
        binding.tvExpenseSummary.text =
            getString(R.string.expense_summary_format, sum, list.size)
    }

    /** expenseId 为 0 时表示新增 */
    private fun openEditor(expenseId: Long) {
        val intent = Intent(requireContext(), ExpenseEditActivity::class.java)
        intent.putExtra(ExpenseEditActivity.EXTRA_EXPENSE_ID, expenseId)
        intent.putExtra(ExpenseEditActivity.EXTRA_VEHICLE_ID, vehicleId)
        startActivity(intent)
    }

    /** 长按删除：费用记录参与成本汇总，误删同样破坏统计口径，故必须二次确认 */
    private fun confirmDelete(expenseId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(R.string.msg_confirm_delete_expense)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repo.deleteExpense(expenseId)
                Toast.makeText(
                    requireContext(), R.string.msg_deleted, Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
