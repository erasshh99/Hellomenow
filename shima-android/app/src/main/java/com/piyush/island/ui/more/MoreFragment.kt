package com.piyush.island.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.piyush.island.R
import com.piyush.island.databinding.FragmentListBinding
import com.piyush.island.databinding.ItemListRowBinding
import com.piyush.island.MainActivity

/**
 * The More tab: the app row, the cards, backup, reliability, Pro and About.
 */
class MoreFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

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
        binding.textTitle.setText(R.string.more_title)
        binding.textSubtitle.text = ""

        val entries = listOf(
            Entry(R.string.more_apps, R.string.more_apps_desc, R.drawable.ic_nav_island) {
                open(AppRowFragment())
            },
            Entry(R.string.more_cards, R.string.more_cards_desc, R.drawable.ic_nav_style) {
                open(CardsFragment())
            },
            Entry(R.string.more_backup, R.string.more_backup_desc, R.drawable.ic_nav_alerts) {
                open(BackupFragment())
            },
            Entry(R.string.more_reliability, R.string.more_reliability_desc, R.drawable.ic_nav_gestures) {
                open(ReliabilityFragment())
            },
            Entry(R.string.more_pro, R.string.more_pro_desc, R.drawable.ic_nav_more) {
                open(ProFragment())
            },
            Entry(R.string.more_about, R.string.more_about_desc, R.drawable.ic_nav_more) {
                open(AboutFragment())
            },
        )

        val adapter = MoreAdapter(entries)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
    }

    private fun open(fragment: Fragment) {
        MainActivity.from(this)?.navigateTo(fragment, fragment::class.java.simpleName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class Entry(
    val titleRes: Int,
    val summaryRes: Int,
    val iconRes: Int,
    val onClick: () -> Unit,
)

class MoreAdapter(
    private val entries: List<Entry>,
) : RecyclerView.Adapter<MoreAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = entries.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        val context = holder.binding.root.context
        holder.binding.textTitle.setText(entry.titleRes)
        holder.binding.textSummary.visibility = View.VISIBLE
        holder.binding.textSummary.setText(entry.summaryRes)
        holder.binding.imageChevron.visibility = View.VISIBLE
        holder.binding.root.setOnClickListener { entry.onClick() }
    }

    class Holder(val binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root)
}
