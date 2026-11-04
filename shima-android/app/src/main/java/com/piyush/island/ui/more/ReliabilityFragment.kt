package com.piyush.island.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentSimpleBinding
import com.piyush.island.overlay.IslandAccessibilityService
import com.piyush.island.service.KeepAliveService
import com.piyush.island.MainActivity

/**
 * Reliability: keep the island alive and find out why it stops.
 */
class ReliabilityFragment : Fragment() {

    private var _binding: FragmentSimpleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSimpleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        heading(getString(R.string.reliability_title))
        body(getString(R.string.reliability_desc))

        val keepAlive = MaterialSwitch(requireContext()).apply {
            text = getString(R.string.keep_alive)
            isChecked = AppGraph.settings.current.keepAlive
            setOnCheckedChangeListener { _, checked ->
                AppGraph.settings.setKeepAlive(checked)
                if (checked) KeepAliveService.start(requireContext()) else KeepAliveService.stop(requireContext())
            }
        }
        binding.container.addView(keepAlive)
        body(getString(R.string.keep_alive_desc))

        val status = TextView(requireContext()).apply {
            textSize = 15f
            setPadding(0, 12, 0, 12)
        }
        binding.container.addView(status)

        val battery = MaterialButton(requireContext()).apply {
            text = getString(R.string.perm_battery_title)
            setOnClickListener { MainActivity.from(this@ReliabilityFragment)?.openBatterySettings() }
        }
        binding.container.addView(battery)

        val accessibility = MaterialButton(requireContext()).apply {
            text = getString(R.string.perm_accessibility_title)
            setOnClickListener { MainActivity.from(this@ReliabilityFragment)?.openAccessibilitySettings() }
        }
        binding.container.addView(accessibility)

        body(getString(R.string.secure_settings_grant, requireContext().packageName))

        renderStatus(status)
    }

    private fun renderStatus(view: TextView) {
        val running = IslandAccessibilityService.instance != null
        view.text = if (running) {
            getString(R.string.island_running)
        } else {
            getString(R.string.reliability_stops)
        }
    }

    private fun heading(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 24, 0, 8)
        binding.container.addView(this)
    }

    private fun body(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 15f
        setPadding(0, 0, 0, 8)
        binding.container.addView(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
