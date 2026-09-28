package com.jtpv.powerconsumption

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jtpv.powerconsumption.data.DbHelper
import com.jtpv.powerconsumption.data.PrefKeys
import com.jtpv.powerconsumption.data.Repository
import com.jtpv.powerconsumption.databinding.FragmentProfileBinding
import java.util.Locale

/** 我的：车辆抬头 + 功能入口 + 关于 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: Repository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(DbHelper(requireContext().applicationContext))

        binding.rowVehicleProfile.setOnClickListener {
            startActivity(Intent(requireContext(), VehicleProfileActivity::class.java))
        }
        binding.rowReminder.setOnClickListener {
            startActivity(Intent(requireContext(), ReminderActivity::class.java))
        }
        binding.rowRefuelParams.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        binding.tvAbout.text = getString(R.string.about_text)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refresh() {
        val vehicles = repo.listVehicles()
        val saved = repo.getPref(PrefKeys.CURRENT_VEHICLE).toLongOrNull()
        val veh = vehicles.firstOrNull { it.id == saved } ?: vehicles.firstOrNull()

        if (veh == null) {
            binding.tvProfileTitle.text = getString(R.string.value_placeholder)
            binding.tvProfileSubtitle.text = getString(R.string.profile_subtitle_empty)
            return
        }

        binding.tvProfileTitle.text = veh.name

        // 副标题按已填写的字段拼装，避免出现「 ·  · 」这类残缺分隔
        val parts = ArrayList<String>()
        if (veh.plateNo.isNotBlank()) parts.add(veh.plateNo)
        if (veh.modelName.isNotBlank()) parts.add(veh.modelName)
        if (veh.odoCurrent > 0L) {
            parts.add(String.format(Locale.US, "%d km", veh.odoCurrent))
        }

        binding.tvProfileSubtitle.text =
            if (parts.isEmpty()) getString(R.string.profile_subtitle_empty)
            else parts.joinToString(" · ")
    }
}
