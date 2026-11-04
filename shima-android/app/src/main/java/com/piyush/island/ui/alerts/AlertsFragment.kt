package com.piyush.island.ui.alerts

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
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentAlertsBinding
import com.piyush.island.databinding.ItemAlertBinding
import com.piyush.island.model.AlertType
import com.piyush.island.MainActivity
import kotlinx.coroutines.launch

/**
 * The list of alerts. Each row switches the alert on and off; tapping the row opens the alert's
 * own settings.
 */
class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AlertsAdapter { type ->
            MainActivity.from(this)?.navigateTo(
                AlertDetailFragment.newInstance(type.id),
                "alert_${type.id}",
            )
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect { adapter.submit(it) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class AlertsAdapter(
    private val onClick: (AlertType) -> Unit,
) : RecyclerView.Adapter<AlertsAdapter.Holder>() {

    private var items: List<AlertType> = emptyList()
    private var enabled: Map<AlertType, Boolean> = emptyMap()

    fun submit(settings: com.piyush.island.model.ShimaSettings) {
        items = AlertType.entries
        enabled = AlertType.entries.associateWith { settings.configFor(it).enabled }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val type = items[position]
        val binding = holder.binding
        binding.textTitle.setText(type.titleRes)
        binding.textSummary.setText(type.summaryRes)
        binding.icon.icon = type.defaultIcon
        binding.icon.tint = type.defaultColor.toInt()
        binding.switchEnabled.setOnCheckedChangeListener(null)
        binding.switchEnabled.isChecked = enabled[type] == true
        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            AppGraph.settings.setAlertEnabled(type, checked)
        }
        binding.root.setOnClickListener { onClick(type) }
    }

    class Holder(val binding: ItemAlertBinding) : RecyclerView.ViewHolder(binding.root)
}
