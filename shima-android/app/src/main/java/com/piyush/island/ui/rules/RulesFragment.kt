package com.piyush.island.ui.rules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentListBinding
import com.piyush.island.databinding.ItemListRowBinding
import com.piyush.island.model.GestureAction
import com.piyush.island.model.Rule
import com.piyush.island.MainActivity
import kotlinx.coroutines.launch

/**
 * Rules let one gesture do different jobs depending on what the phone is doing.
 */
class RulesFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RuleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textTitle.setText(R.string.rules_title)
        binding.textSubtitle.setText(R.string.rules_subtitle)
        binding.fab.visibility = View.VISIBLE
        binding.fab.setOnClickListener {
            MainActivity.from(this)?.navigateTo(RuleEditorFragment.newInstance(null), "rule_new")
        }

        adapter = RuleAdapter { rule ->
            MainActivity.from(this)?.navigateTo(RuleEditorFragment.newInstance(rule.id), "rule_${rule.id}")
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect { adapter.submit(it.rules) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class RuleAdapter(
    private val onClick: (Rule) -> Unit,
) : RecyclerView.Adapter<RuleAdapter.Holder>() {

    private var items: List<Rule> = emptyList()

    fun submit(rules: List<Rule>) {
        items = rules
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val rule = items[position]
        val context = holder.binding.root.context
        holder.binding.textTitle.text = context.getString(rule.gesture.labelRes)
        holder.binding.textSummary.visibility = View.VISIBLE
        holder.binding.textSummary.text = rule.conditions
            .joinToString(", ") { context.getString(it.labelRes) }
            .ifEmpty { "Always" }
        holder.binding.textValue.visibility = View.VISIBLE
        holder.binding.textValue.text = when (val action = rule.action) {
            is GestureAction.None -> context.getString(R.string.gesture_none)
            is GestureAction.LaunchApp -> action.label
            is GestureAction.OpenWidget -> action.label
            is GestureAction.SystemAction -> context.getString(action.action.labelRes)
            is GestureAction.TaskerTask -> action.taskName
        }
        holder.binding.root.setOnClickListener { onClick(rule) }
    }

    class Holder(val binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root)
}
