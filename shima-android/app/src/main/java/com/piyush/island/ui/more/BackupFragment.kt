package com.piyush.island.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.data.AutoBackupJob
import com.piyush.island.data.BackupManager
import com.piyush.island.databinding.FragmentSimpleBinding

/**
 * Backup: save the whole setup to a file and put it back later.
 */
class BackupFragment : Fragment() {

    private var _binding: FragmentSimpleBinding? = null
    private val binding get() = _binding!!

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val json = BackupManager.exportJson(AppGraph.settings.current)
        runCatching {
            requireContext().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        }.onSuccess {
            status(getString(R.string.backup_saved))
        }.onFailure {
            status(getString(R.string.backup_failed))
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val raw = runCatching {
            requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (raw.isNullOrEmpty() || !BackupManager.importJson(AppGraph.settings, raw)) {
            status(getString(R.string.backup_failed))
        } else {
            status(getString(R.string.backup_restored))
        }
    }

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

        heading(getString(R.string.more_backup))
        body(getString(R.string.backup_desc))

        val export = MaterialButton(requireContext()).apply {
            text = getString(R.string.backup_export)
            setOnClickListener { exportLauncher.launch(BackupManager.FILE_NAME) }
        }
        binding.container.addView(export)

        val import = MaterialButton(requireContext()).apply {
            text = getString(R.string.backup_import)
            setOnClickListener { importLauncher.launch(arrayOf("application/json", "text/plain")) }
        }
        binding.container.addView(import)

        val autoSwitch = MaterialSwitch(requireContext()).apply {
            text = getString(R.string.backup_auto)
            isChecked = AppGraph.settings.current.autoBackup
            setOnCheckedChangeListener { _, checked ->
                AppGraph.settings.setAutoBackup(checked)
                if (checked) AutoBackupJob.schedule(requireContext()) else AutoBackupJob.cancel(requireContext())
            }
        }
        binding.container.addView(autoSwitch)
        body(getString(R.string.backup_auto_desc))

        status("")
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

    private fun status(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 15f
        setPadding(0, 16, 0, 0)
        binding.container.addView(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
