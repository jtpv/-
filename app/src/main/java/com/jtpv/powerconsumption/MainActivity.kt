package com.jtpv.powerconsumption

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationBarView
import com.jtpv.powerconsumption.databinding.ActivityMainBinding

/**
 * 主容器：承载五个功能页并切换显示。
 *
 * 采用「一次性全部 add 后切换 show/hide」而非 replace：
 * replace 会销毁上一个 Fragment 的视图，回来时滚动位置与输入内容全部丢失，
 * 对记录类应用是明显的体验缺陷。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var homeFragment: Fragment
    private lateinit var recordFragment: Fragment
    private lateinit var expenseFragment: Fragment
    private lateinit var reportFragment: Fragment
    private lateinit var profileFragment: Fragment

    private var currentNavId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            recordFragment = RecordFragment()
            expenseFragment = ExpenseFragment()
            reportFragment = ReportFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, TAG_PROFILE).hide(profileFragment)
                .add(R.id.fragmentContainer, reportFragment, TAG_REPORT).hide(reportFragment)
                .add(R.id.fragmentContainer, expenseFragment, TAG_EXPENSE).hide(expenseFragment)
                .add(R.id.fragmentContainer, recordFragment, TAG_RECORD).hide(recordFragment)
                .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                .commit()
        } else {
            /*
             * 配置变更或进程恢复后，Fragment 由 FragmentManager 自动重建。
             * 必须按 tag 取回既有实例：若直接 new，会得到未被添加到管理器的孤儿对象，
             * 后续 show/hide 会抛 IllegalArgumentException。
             */
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) ?: HomeFragment()
            recordFragment = supportFragmentManager.findFragmentByTag(TAG_RECORD) ?: RecordFragment()
            expenseFragment = supportFragmentManager.findFragmentByTag(TAG_EXPENSE) ?: ExpenseFragment()
            reportFragment = supportFragmentManager.findFragmentByTag(TAG_REPORT) ?: ReportFragment()
            profileFragment = supportFragmentManager.findFragmentByTag(TAG_PROFILE) ?: ProfileFragment()
        }

        binding.bottomNav.setOnItemSelectedListener(
            object : NavigationBarView.OnItemSelectedListener {
                override fun onNavigationItemSelected(item: MenuItem): Boolean {
                    val id = item.itemId
                    if (id == R.id.nav_home || id == R.id.nav_record ||
                        id == R.id.nav_expense || id == R.id.nav_report ||
                        id == R.id.nav_profile
                    ) {
                        switchTo(id)
                        return true
                    }
                    return false
                }
            }
        )

        // 导航选中项与 Fragment 显隐由不同机制各自恢复，可能不同步，此处强制对齐
        switchTo(binding.bottomNav.selectedItemId)
    }

    /** 切换可见页。重复点选当前页时直接返回，避免无谓的事务提交 */
    private fun switchTo(navId: Int) {
        val target = fragmentFor(navId)
        title = titleFor(navId)

        if (navId == currentNavId && !target.isHidden) return
        currentNavId = navId

        val tx = supportFragmentManager.beginTransaction()
        for (f in allFragments()) {
            if (f === target) {
                if (f.isHidden) tx.show(f)
            } else if (!f.isHidden) {
                tx.hide(f)
            }
        }
        tx.commit()
    }

    private fun fragmentFor(navId: Int): Fragment = when (navId) {
        R.id.nav_record -> recordFragment
        R.id.nav_expense -> expenseFragment
        R.id.nav_report -> reportFragment
        R.id.nav_profile -> profileFragment
        else -> homeFragment
    }

    private fun allFragments(): List<Fragment> = listOf(
        homeFragment, recordFragment, expenseFragment, reportFragment, profileFragment
    )

    private fun titleFor(navId: Int): String = when (navId) {
        R.id.nav_record -> getString(R.string.nav_record)
        R.id.nav_expense -> getString(R.string.nav_expense)
        R.id.nav_report -> getString(R.string.nav_report)
        R.id.nav_profile -> getString(R.string.nav_profile)
        else -> getString(R.string.nav_home)
    }

    companion object {
        private const val TAG_HOME = "tab_home"
        private const val TAG_RECORD = "tab_record"
        private const val TAG_EXPENSE = "tab_expense"
        private const val TAG_REPORT = "tab_report"
        private const val TAG_PROFILE = "tab_profile"
    }
}
