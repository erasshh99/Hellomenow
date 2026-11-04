package com.piyush.island.ui.alerts

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.DialogAppPickerBinding
import com.piyush.island.databinding.ItemAppBinding
import com.piyush.island.model.AlertType

/**
 * The settings of one alert: how long it stays, how it closes, which icon and colour it uses and
 * which apps are allowed to raise it.
 */
class AlertDetailFragment : androidx.fragment.app.Fragment() {

    private var _binding: com.piyush.island.databinding.FragmentAlertDetailBinding? = null
    private val binding get() = _binding!!

    private var type: AlertType = AlertType.MEDIA

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = com.piyush.island.databinding.FragmentAlertDetailBinding.inflate(
            inflater,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        type = AlertType.fromId(arguments?.getString(ARG_TYPE).orEmpty()) ?: AlertType.MEDIA
        binding.textTitle.setText(type.titleRes)

        binding.buttonBack.setOnClickListener { parentFragmentManager.popBackStack() }

        buildAnimationChips()
        buildIconChips()
        buildColorChips()
        render()

        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            AppGraph.settings.setAlertEnabled(type, checked)
        }
        binding.sliderDuration.addOnChangeListener { _, value, fromUser ->
            if (fromUser) AppGraph.settings.updateAlert(type) { it.copy(durationMs = value.toInt()) }
        }
        binding.groupClose.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_tap -> com.piyush.island.model.CloseMode.TAP_OUTSIDE
                R.id.radio_swipe -> com.piyush.island.model.CloseMode.SWIPE_UP
                R.id.radio_never -> com.piyush.island.model.CloseMode.NEVER
                else -> com.piyush.island.model.CloseMode.BOTH
            }
            AppGraph.settings.updateAlert(type) { it.copy(closeMode = mode) }
        }
        binding.buttonApps.setOnClickListener {
            AppPickerDialog.newInstance(type.id).show(parentFragmentManager, "apps")
        }
        binding.buttonRestore.setOnClickListener {
            AppGraph.settings.updateAlert(type) { AlertConfigDefaults.of(type) }
            render()
        }
    }

    private fun render() {
        val config = AppGraph.settings.current.configFor(type)
        binding.switchEnabled.isChecked = config.enabled
        binding.sliderDuration.value = config.durationMs.toFloat().coerceIn(2000f, 30000f)
        binding.groupClose.check(
            when (config.closeMode) {
                com.piyush.island.model.CloseMode.TAP_OUTSIDE -> R.id.radio_tap
                com.piyush.island.model.CloseMode.SWIPE_UP -> R.id.radio_swipe
                com.piyush.island.model.CloseMode.NEVER -> R.id.radio_never
                com.piyush.island.model.CloseMode.BOTH -> R.id.radio_both
            }
        )
        for (index in 0 until binding.groupAnimation.childCount) {
            val chip = binding.groupAnimation.getChildAt(index) as com.google.android.material.chip.Chip
            chip.isChecked = chip.tag == config.animation
        }
        for (index in 0 until binding.groupIcon.childCount) {
            val chip = binding.groupIcon.getChildAt(index) as com.google.android.material.chip.Chip
            chip.isChecked = chip.tag == config.icon
        }
        for (index in 0 until binding.groupColor.childCount) {
            val chip = binding.groupColor.getChildAt(index) as com.google.android.material.chip.Chip
            chip.isChecked = (chip.tag as? Long) == config.color
        }
        binding.textApps.setText(
            if (config.packages.isEmpty()) R.string.alert_apps_all else R.string.alert_apps_none
        )
        binding.textApps.text = if (config.packages.isEmpty()) {
            getString(R.string.alert_apps_all)
        } else {
            "${config.packages.size} apps can show this alert"
        }
    }

    private fun buildAnimationChips() {
        com.piyush.island.model.IslandAnimation.entries.forEach { animation ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = getString(animation.labelRes)
            chip.tag = animation
            chip.isCheckable = true
            chip.setOnClickListener {
                AppGraph.settings.updateAlert(type) { it.copy(animation = animation) }
            }
            binding.groupAnimation.addView(chip)
        }
    }

    private fun buildIconChips() {
        ICONS.forEach { icon ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = icon.replaceFirstChar { it.uppercase() }
            chip.tag = icon
            chip.isCheckable = true
            chip.setOnClickListener {
                AppGraph.settings.updateAlert(type) { it.copy(icon = icon) }
            }
            binding.groupIcon.addView(chip)
        }
    }

    private fun buildColorChips() {
        COLORS.forEach { color ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = "#%06X".format(color and 0xFFFFFF)
            chip.tag = color
            chip.isCheckable = true
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(color.toInt())
            chip.setOnClickListener {
                AppGraph.settings.updateAlert(type) { it.copy(color = color) }
            }
            binding.groupColor.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(typeId: String) = AlertDetailFragment().apply {
            arguments = Bundle().apply { putString(ARG_TYPE, typeId) }
        }
    }
}

/** The values an alert falls back to when the user restores the defaults. */
object AlertConfigDefaults {
    fun of(type: AlertType) = com.piyush.island.model.AlertConfig(
        enabled = false,
        icon = type.defaultIcon,
        color = type.defaultColor,
        animation = com.piyush.island.model.IslandAnimation.SPRING,
        durationMs = 6_000,
        closeMode = com.piyush.island.model.CloseMode.BOTH,
    )
}

private val ICONS = listOf(
    "music", "battery", "battery_low", "battery_full", "torch", "timer", "volume",
    "brightness", "mic", "camera", "dnd", "usb", "signal", "alarm", "rotate", "record",
    "shot", "app",
)

private val COLORS = listOf(
    0xFF7C5CFF, 0xFF2E9E5B, 0xFFF2A33C, 0xFF2D8CF0, 0xFF3AAFA9, 0xFFD1495B,
    0xFF6C5CE7, 0xFFE24A3A, 0xFF6E8B3D, 0xFF8A8F98, 0xFF4B6BFB, 0xFF4E8A8A,
)

/**
 * Lets the user choose which apps may raise an alert. With nothing picked every app that has
 * notification access may show it.
 */
class AppPickerDialog : DialogFragment() {

    private var _binding: DialogAppPickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppAdapter
    private var type: AlertType = AlertType.MEDIA

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAppPickerBinding.inflate(layoutInflater)
        type = AlertType.fromId(arguments?.getString(ARG_TYPE).orEmpty()) ?: AlertType.MEDIA

        adapter = AppAdapter(requireContext()) { packageName, checked ->
            val current = AppGraph.settings.current.configFor(type).packages.toMutableSet()
            if (checked) current.add(packageName) else current.remove(packageName)
            AppGraph.settings.setAlertPackages(type, current)
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        adapter.submit(AppGraph.settings.current.configFor(type).packages)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alert_apps)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(typeId: String) = AppPickerDialog().apply {
            arguments = Bundle().apply { putString(ARG_TYPE, typeId) }
        }
    }
}

class AppAdapter(
    private val context: android.content.Context,
    private val onToggle: (String, Boolean) -> Unit,
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private var items: List<InstalledApp> = emptyList()
    private var selected: Set<String> = emptySet()

    fun submit(selectedPackages: Set<String>) {
        selected = selectedPackages
        items = InstalledApps.list(context)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = items[position]
        holder.binding.textTitle.text = app.label
        holder.binding.textSummary.text = app.packageName
        holder.binding.switchEnabled.setOnCheckedChangeListener(null)
        holder.binding.switchEnabled.isChecked = app.packageName in selected
        holder.binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            onToggle(app.packageName, checked)
        }
    }

    class Holder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}

data class InstalledApp(val label: String, val packageName: String)

object InstalledApps {
    fun list(context: android.content.Context): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                InstalledApp(
                    label = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
