package com.piyush.island.ui.more

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentListBinding
import com.piyush.island.databinding.ItemListRowBinding
import com.piyush.island.model.AlertType
import kotlinx.coroutines.launch

/**
 * The cards tab: for every alert the user picks the parts the card shows.
 */
class CardsFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CardsAdapter

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
        binding.textTitle.setText(R.string.cards_title)
        binding.textSubtitle.text = ""
        binding.buttonBack.visibility = View.VISIBLE
        binding.buttonBack.setOnClickListener { parentFragmentManager.popBackStack() }

        adapter = CardsAdapter { type -> pickParts(type) }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect { adapter.submit(it.decks) }
            }
        }
    }

    private fun pickParts(type: AlertType) {
        val decks = AppGraph.settings.current.decks
        val current = decks[type.id]?.toMutableList() ?: defaultParts(type).toMutableList()
        val labels = PARTS.map { getString(it.second) }.toTypedArray()
        val checked = PARTS.map { current.contains(it.first) }.toBooleanArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.customize_alert, getString(type.titleRes)))
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                if (isChecked) {
                    if (!current.contains(PARTS[which].first)) current.add(PARTS[which].first)
                } else {
                    current.remove(PARTS[which].first)
                }
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                AppGraph.settings.setDeck(type.id, current)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun defaultParts(type: AlertType): List<String> = when (type) {
        AlertType.MEDIA -> listOf("title", "subtitle", "artwork", "progress", "actions")
        AlertType.LIVE -> listOf("title", "subtitle", "progress", "actions")
        AlertType.CHARGING -> listOf("title", "subtitle", "progress")
        else -> listOf("title", "subtitle")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val PARTS = listOf(
            "title" to R.string.alert_title_part,
            "subtitle" to R.string.alert_subtitle_part,
            "progress" to R.string.alert_progress_part,
            "artwork" to R.string.alert_artwork_part,
            "lyric" to R.string.alert_lyric_part,
            "actions" to R.string.alert_actions_part,
        )
    }
}

class CardsAdapter(
    private val onClick: (AlertType) -> Unit,
) : RecyclerView.Adapter<CardsAdapter.Holder>() {

    private var decks: Map<String, List<String>> = emptyMap()

    fun submit(decks: Map<String, List<String>>) {
        this.decks = decks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = AlertType.entries.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val type = AlertType.entries[position]
        val context = holder.binding.root.context
        holder.binding.textTitle.setText(type.titleRes)
        holder.binding.textSummary.visibility = View.VISIBLE
        holder.binding.textSummary.setText(type.summaryRes)
        val parts = decks[type.id].orEmpty()
        holder.binding.textValue.visibility = View.VISIBLE
        holder.binding.textValue.text = if (parts.isEmpty()) {
            context.getString(R.string.card_parts_default)
        } else {
            parts.joinToString(", ")
        }
        holder.binding.root.setOnClickListener { onClick(type) }
    }

    class Holder(val binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root)
}
