package com.piyush.island.ui.island

import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentIslandBinding
import com.piyush.island.island.IslandCard
import com.piyush.island.model.AlertType
import com.piyush.island.model.ThemeMode
import com.piyush.island.overlay.IslandAccessibilityService
import com.piyush.island.MainActivity
import kotlinx.coroutines.launch

/**
 * The first tab: a live preview of the island, the master switch and the permissions the island
 * needs to work.
 */
class IslandFragment : Fragment() {

    private var _binding: FragmentIslandBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentIslandBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            AppGraph.settings.setEnabled(checked)
            updateStatus()
        }

        binding.buttonAccessibility.setOnClickListener {
            MainActivity.from(this)?.openAccessibilitySettings()
        }
        binding.buttonNotification.setOnClickListener {
            MainActivity.from(this)?.openNotificationListenerSettings()
        }
        binding.buttonBattery.setOnClickListener {
            MainActivity.from(this)?.openBatterySettings()
        }
        binding.buttonTry.setOnClickListener { showDemo() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect { render() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissions()
    }

    private fun render() {
        val settings = AppGraph.settings.current
        binding.switchEnabled.isChecked = settings.enabled
        val dark = when (settings.style.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        binding.preview.render(settings.style, dark)
        updateStatus()
    }

    private fun updateStatus() {
        val running = AppGraph.settings.current.enabled && IslandAccessibilityService.instance != null
        binding.textStatus.setText(if (running) R.string.island_running else R.string.island_stopped)
    }

    private fun updatePermissions() {
        val accessibilityOn = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().contains(requireContext().packageName)

        val notificationOn = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners",
        ).orEmpty().contains(requireContext().packageName)

        binding.textAccessibility.setText(
            if (accessibilityOn) R.string.granted else R.string.perm_accessibility_body
        )
        binding.buttonAccessibility.setText(
            if (accessibilityOn) R.string.granted else R.string.grant
        )
        binding.buttonAccessibility.isEnabled = !accessibilityOn

        binding.textNotification.setText(
            if (notificationOn) R.string.granted else R.string.perm_notification_body
        )
        binding.buttonNotification.setText(
            if (notificationOn) R.string.granted else R.string.grant
        )
        binding.buttonNotification.isEnabled = !notificationOn

        updateStatus()
    }

    /** Shows a card on the island so the user can see the shape without playing music first. */
    private fun showDemo() {
        val card = IslandCard(
            type = AlertType.MEDIA,
            title = "Example track",
            subtitle = "Example artist",
            icon = "music",
            color = 0xFF7C5CFF.toInt(),
            accent = 0xFF8AD5C6.toInt(),
            progress = 0.4f,
            autoDismissAfterMs = 8_000,
        )
        AppGraph.islandState.show(card)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
