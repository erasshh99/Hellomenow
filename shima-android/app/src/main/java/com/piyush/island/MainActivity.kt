package com.piyush.island

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.ActivityMainBinding
import com.piyush.island.island.CutoutInfo
import com.piyush.island.overlay.IslandAccessibilityService
import com.piyush.island.service.KeepAliveService
import com.piyush.island.service.WatchdogJob
import com.piyush.island.ui.alerts.AlertDetailFragment
import com.piyush.island.ui.alerts.AlertsFragment
import com.piyush.island.ui.gestures.ActionPickerFragment
import com.piyush.island.ui.gestures.GesturesFragment
import com.piyush.island.ui.island.IslandFragment
import com.piyush.island.ui.more.AboutFragment
import com.piyush.island.ui.more.AppRowFragment
import com.piyush.island.ui.more.BackupFragment
import com.piyush.island.ui.more.CardsFragment
import com.piyush.island.ui.more.MoreFragment
import com.piyush.island.ui.more.ProFragment
import com.piyush.island.ui.more.ReliabilityFragment
import com.piyush.island.ui.rules.RuleEditorFragment
import com.piyush.island.ui.rules.RulesFragment
import com.piyush.island.ui.style.StyleFragment
import kotlinx.coroutines.launch

/**
 * The single activity. It owns the bottom navigation, hands the camera cutout to the overlay so
 * the island can line up with it, and keeps the keep-alive service in step with the settings.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) updateKeepAlive()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.install(applicationContext)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation(savedInstanceState)
        reportCutout()
        requestNotificationPermissionIfNeeded()
        WatchdogJob.schedule(this)

        if (!AppGraph.settings.current.onboardingDone) {
            showOnboarding()
        }
    }

    override fun onResume() {
        super.onResume()
        reportCutout()
        updateKeepAlive()
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_island -> IslandFragment()
                R.id.nav_alerts -> AlertsFragment()
                R.id.nav_style -> StyleFragment()
                R.id.nav_gestures -> GesturesFragment()
                else -> MoreFragment()
            }
            show(fragment, item.itemId.toString())
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_island
        }
    }

    private fun show(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, tag)
        }
    }

    /** Sends the camera cutout to the overlay so the island can be lined up with it. */
    private fun reportCutout() {
        val root = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val info = CutoutInfo(
                left = cutout.left,
                right = cutout.right,
                top = cutout.top,
                bottom = cutout.bottom,
                displayWidth = root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels,
                displayHeight = root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels,
            )
            IslandAccessibilityService.instance?.updateCutout(info)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun updateKeepAlive() {
        val settings = AppGraph.settings.current
        if (settings.enabled && settings.keepAlive) {
            KeepAliveService.start(this)
        } else {
            KeepAliveService.stop(this)
        }
    }

    private fun showOnboarding() {
        val fragment = com.piyush.island.ui.onboarding.OnboardingFragment()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, "onboarding")
            addToBackStack("onboarding")
        }
        binding.bottomNav.visibility = View.GONE
    }

    fun finishOnboarding() {
        AppGraph.settings.setOnboardingDone(true)
        supportFragmentManager.popBackStack()
        binding.bottomNav.visibility = View.VISIBLE
    }

    /** Opens a screen that is not a bottom navigation tab. */
    fun navigateTo(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, tag)
            addToBackStack(tag)
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        runCatching { startActivity(intent) }
    }

    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        runCatching { startActivity(intent) }
    }

    fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        runCatching { startActivity(intent) }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", packageName, null))
        runCatching { startActivity(intent) }
    }

    companion object {
        /** Lets a fragment ask the activity for something that belongs to it. */
        fun from(fragment: Fragment): MainActivity? = fragment.activity as? MainActivity
    }
}
