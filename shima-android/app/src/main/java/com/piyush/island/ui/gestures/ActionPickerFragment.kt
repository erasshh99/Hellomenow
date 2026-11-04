package com.piyush.island.ui.gestures

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.DialogAppPickerBinding
import com.piyush.island.databinding.ItemListRowBinding
import com.piyush.island.gestures.AppWidgetHostBridge
import com.piyush.island.model.Gesture
import com.piyush.island.model.GestureAction
import com.piyush.island.model.Surface
import com.piyush.island.model.SystemActionKind
import com.piyush.island.ui.alerts.InstalledApps

/**
 * Lets the user pick what a gesture does: nothing, a system action, an app or a widget.
 */
class ActionPickerFragment : DialogFragment() {

    private var _binding: DialogAppPickerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ActionAdapter
    private var surface: Surface = Surface.COLLAPSED
    private var gesture: Gesture = Gesture.TAP

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAppPickerBinding.inflate(layoutInflater)
        surface = runCatching {
            Surface.valueOf(arguments?.getString(ARG_SURFACE).orEmpty())
        }.getOrDefault(Surface.COLLAPSED)
        gesture = Gesture.fromId(arguments?.getString(ARG_GESTURE).orEmpty()) ?: Gesture.TAP

        val options = buildOptions()
        adapter = ActionAdapter { action ->
            val ruleId = arguments?.getString(ARG_RULE_ID)
            if (ruleId.isNullOrEmpty()) {
                AppGraph.settings.setGesture(surface, gesture, action)
            } else {
                AppGraph.settings.update { settings ->
                    settings.copy(
                        rules = settings.rules.map { rule ->
                            if (rule.id == ruleId) rule.copy(action = action) else rule
                        },
                    )
                }
            }
            dismiss()
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        adapter.submit(options)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.action_pick_title, getString(gesture.labelRes)))
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun buildOptions(): List<ActionOption> {
        val options = mutableListOf(
            ActionOption(getString(R.string.gesture_none), GestureAction.None),
        )
        SystemActionKind.entries.forEach { kind ->
            options.add(ActionOption(getString(kind.labelRes), GestureAction.SystemAction(kind)))
        }

        val apps = InstalledApps.list(requireContext())
        apps.forEach { app ->
            options.add(
                ActionOption(
                    getString(R.string.action_open_app) + ": " + app.label,
                    GestureAction.LaunchApp(app.packageName, app.label),
                )
            )
        }

        AppWidgetHostBridge.widgets(requireContext()).forEach { widget ->
            options.add(
                ActionOption(
                    getString(R.string.action_open_widget) + ": " + widget.label,
                    GestureAction.OpenWidget(widget.id, widget.label),
                )
            )
        }
        return options
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SURFACE = "surface"
        private const val ARG_GESTURE = "gesture"
        private const val ARG_RULE_ID = "rule_id"

        fun newInstance(surface: String, gesture: String) = ActionPickerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SURFACE, surface)
                putString(ARG_GESTURE, gesture)
            }
        }

        /** Picks the action of a rule instead of a plain gesture binding. */
        fun forRule(ruleId: String, gesture: String) = ActionPickerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SURFACE, Surface.COLLAPSED.name)
                putString(ARG_GESTURE, gesture)
                putString(ARG_RULE_ID, ruleId)
            }
        }
    }
}

data class ActionOption(val label: String, val action: GestureAction)

class ActionAdapter(
    private val onPick: (GestureAction) -> Unit,
) : RecyclerView.Adapter<ActionAdapter.Holder>() {

    private var items: List<ActionOption> = emptyList()

    fun submit(options: List<ActionOption>) {
        items = options
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val option = items[position]
        holder.binding.textTitle.text = option.label
        holder.binding.root.setOnClickListener { onPick(option.action) }
    }

    class Holder(val binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root)
}
