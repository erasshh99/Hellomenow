package com.piyush.island.ui.rules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentRuleEditorBinding
import com.piyush.island.model.Gesture
import com.piyush.island.model.GestureAction
import com.piyush.island.model.Rule
import com.piyush.island.model.RuleCondition
import com.piyush.island.ui.gestures.ActionPickerFragment
import java.util.UUID

/**
 * The editor for one rule: which gesture it applies to, the conditions that have to be true and
 * what the gesture does when they are.
 */
class RuleEditorFragment : Fragment() {

    private var _binding: FragmentRuleEditorBinding? = null
    private val binding get() = _binding!!

    private var ruleId: String? = null
    private var gesture: Gesture = Gesture.TAP
    private var conditions: Set<RuleCondition> = emptySet()
    private var action: GestureAction = GestureAction.None
    private var requireUnlock = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRuleEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ruleId = arguments?.getString(ARG_RULE_ID)

        val existing = ruleId?.let { id -> AppGraph.settings.current.rules.firstOrNull { it.id == id } }
        if (existing != null) {
            gesture = existing.gesture
            conditions = existing.conditions
            action = existing.action
            requireUnlock = existing.requireUnlock
            binding.textTitle.setText(R.string.edit_rule)
            binding.buttonDelete.visibility = View.VISIBLE
        } else {
            binding.textTitle.setText(R.string.new_rule)
        }

        binding.buttonBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.buttonDelete.setOnClickListener {
            ruleId?.let { AppGraph.settings.deleteRule(it) }
            parentFragmentManager.popBackStack()
        }
        binding.switchUnlock.isChecked = requireUnlock
        binding.switchUnlock.setOnCheckedChangeListener { _, checked -> requireUnlock = checked }

        buildGestureChips()
        buildConditionChips()
        binding.buttonAction.setOnClickListener {
            val id = ruleId ?: "new"
            ActionPickerFragment.forRule(id, gesture.id).show(parentFragmentManager, "rule_action")
        }
        binding.buttonSave.setOnClickListener { save() }

        renderAction()
    }

    private fun buildGestureChips() {
        Gesture.entries.forEach { option ->
            val chip = Chip(requireContext())
            chip.text = getString(option.labelRes)
            chip.tag = option
            chip.isCheckable = true
            chip.isChecked = option == gesture
            chip.setOnClickListener {
                gesture = option
                renderAction()
            }
            binding.groupGesture.addView(chip)
        }
    }

    private fun buildConditionChips() {
        RuleCondition.entries.forEach { condition ->
            val chip = Chip(requireContext())
            chip.text = getString(condition.labelRes)
            chip.tag = condition
            chip.isCheckable = true
            chip.isChecked = condition in conditions
            chip.setOnCheckedChangeListener { _, checked ->
                conditions = if (checked) conditions + condition else conditions - condition
            }
            binding.groupConditions.addView(chip)
        }
    }

    private fun renderAction() {
        val current = action
        binding.buttonAction.text = when (current) {
            is GestureAction.None -> getString(R.string.gesture_none)
            is GestureAction.LaunchApp -> current.label
            is GestureAction.OpenWidget -> current.label
            is GestureAction.SystemAction -> getString(current.action.labelRes)
            is GestureAction.TaskerTask -> current.taskName
        }
    }

    private fun save() {
        val id = ruleId ?: UUID.randomUUID().toString()
        val rule = Rule(
            id = id,
            gesture = gesture,
            conditions = conditions,
            action = action,
            requireUnlock = requireUnlock,
            enabled = true,
        )
        AppGraph.settings.upsertRule(rule)
        ruleId = id
        parentFragmentManager.popBackStack()
    }

    override fun onResume() {
        super.onResume()
        val id = ruleId ?: return
        val rule = AppGraph.settings.current.rules.firstOrNull { it.id == id } ?: return
        action = rule.action
        renderAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RULE_ID = "rule_id"

        fun newInstance(ruleId: String?) = RuleEditorFragment().apply {
            arguments = Bundle().apply { putString(ARG_RULE_ID, ruleId) }
        }
    }
}
