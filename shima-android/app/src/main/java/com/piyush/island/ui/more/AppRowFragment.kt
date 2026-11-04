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
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentListBinding
import com.piyush.island.databinding.ItemListRowBinding
import com.piyush.island.ui.alerts.AppAdapter
import com.piyush.island.ui.alerts.InstalledApps
import kotlinx.coroutines.launch

/**
 * The app row: the apps that sit under the island. It holds up to ten of them.
 */
class AppRowFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppAdapter

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
        binding.textTitle.setText(R.string.app_row_title)
        binding.textSubtitle.setText(R.string.app_row_desc)
        binding.buttonBack.visibility = View.VISIBLE
        binding.buttonBack.setOnClickListener { parentFragmentManager.popBackStack() }

        adapter = AppAdapter(requireContext()) { packageName, checked ->
            val current = AppGraph.settings.current.appRow.toMutableList()
            if (checked) {
                if (current.size >= com.piyush.island.data.SettingsStore.MAX_APP_ROW) return@AppAdapter
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            AppGraph.settings.setAppRow(current)
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect {
                    adapter.submit(it.appRow.toSet())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
