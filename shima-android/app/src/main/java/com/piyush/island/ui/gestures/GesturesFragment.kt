package com.piyush.island.ui.gestures

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
import com.piyush.island.model.Gesture
import com.piyush.island.model.GestureAction
import com.piyush.island.model.Surface
import kotlinx.coroutines.launch

/**
 * The Gestures tab. Every gesture the island knows is listed twice: once for the collapsed
 * island and once for the open card.
 */
class GesturesFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GestureAdapter

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
        binding.textTitle.setText(R.string.gestures_title)
        binding.textSubtitle.setText(R.string.gestures_subtitle)

        adapter = GestureAdapter { surface, gesture ->
            ActionPickerFragment.newInstance(surface.name, gesture.id)
                .show(parentFragmentManager, "action")
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect { adapter.submit(it.gestures) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class GestureAdapter(
    private val onClick: (Surface, Gesture) -> Unit,
) : RecyclerView.Adapter<GestureAdapter.Holder>() {

    private data class Row(val surface: Surface, val gesture: Gesture, val action: GestureAction)

    private var rows: List<Row> = emptyList()

    fun submit(bindings: com.piyush.island.model.GestureBindings) {
        rows = buildList {
            Surface.entries.forEach { surface ->
                Gesture.entries.forEach { gesture ->
                    add(Row(surface, gesture, bindings.actionFor(surface, gesture)))
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        val context = holder.binding.root.context
        holder.binding.textTitle.text = context.getString(row.gesture.labelRes)
        holder.binding.textSummary.visibility = android.view.View.VISIBLE
        holder.binding.textSummary.text = when (row.surface) {
            Surface.COLLAPSED -> context.getString(R.string.gesture_collapsed)
            Surface.EXPANDED -> context.getString(R.string.gesture_expanded)
        }
        holder.binding.textValue.visibility = android.view.View.VISIBLE
        holder.binding.textValue.text = when (val action = row.action) {
            is GestureAction.None -> context.getString(R.string.gesture_none)
            is GestureAction.LaunchApp -> action.label
            is GestureAction.OpenWidget -> action.label
            is GestureAction.SystemAction -> context.getString(action.action.labelRes)
            is GestureAction.TaskerTask -> action.taskName
        }
        holder.binding.root.setOnClickListener { onClick(row.surface, row.gesture) }
    }

    class Holder(val binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root)
}
