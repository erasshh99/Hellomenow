package com.piyush.island.ui.more

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.piyush.island.BuildConfig
import com.piyush.island.R
import com.piyush.island.data.Changelog
import com.piyush.island.databinding.FragmentSimpleBinding
import com.piyush.island.MainActivity
import org.json.JSONArray

/**
 * About: the version, what changed, what Shima reads and the licences of everything it is
 * built on.
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentSimpleBinding? = null
    private val binding get() = _binding!!

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

        heading(getString(R.string.app_name))
        body(
            getString(
                R.string.about_build,
                getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME,
                android.os.Build.VERSION.RELEASE,
                android.os.Build.VERSION.SDK_INT,
            )
        )
        body(getString(R.string.app_tagline))

        addLink(
            getString(R.string.about_update),
            getString(R.string.about_checking),
        ) {
            checkForUpdates()
        }

        heading(getString(R.string.about_privacy))
        body(getString(R.string.about_privacy_summary))
        body(getString(R.string.about_no_account))
        body(getString(R.string.about_no_analytics))
        body(getString(R.string.about_no_ads))
        body(getString(R.string.about_on_device))

        val entries = Changelog.load(requireContext())
        val current = Changelog.forVersion(entries, BuildConfig.VERSION_NAME)
        if (current != null) {
            heading(getString(R.string.about_whats_new, current.version))
            body(current.summary)
            current.changes.forEach { change ->
                body("• [${change.kind}] ${change.text}")
            }
        }

        heading(getString(R.string.about_licences))
        body(getString(R.string.about_licences_desc))
        licences().take(12).forEach { entry ->
            body("• $entry")
        }

        val about = MaterialButton(requireContext()).apply {
            text = getString(R.string.about_mail)
            setOnClickListener {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_SENDTO,
                    android.net.Uri.parse("mailto:oto.music121@gmail.com?subject=Shima%20${BuildConfig.VERSION_NAME}"),
                )
                runCatching { startActivity(intent) }
            }
        }
        binding.container.addView(about)
    }

    private fun licences(): List<String> = try {
        val raw = requireContext().assets.open("licenses.json").bufferedReader().use { it.readText() }
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val group = item.optString("groupId")
            val artifact = item.optString("artifactId")
            val version = item.optString("version")
            val name = item.optString("name")
            "$name ($group:$artifact $version)"
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun checkForUpdates() {
        val manager = AppUpdateManagerFactory.create(requireContext())
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    val host = activity ?: return@addOnSuccessListener
                    val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    runCatching {
                        manager.startUpdateFlowForResult(info, host, options, REQUEST_UPDATE)
                    }.onFailure {
                        body(getString(R.string.about_update_failed))
                    }
                } else {
                    body(getString(R.string.about_latest))
                }
            }
            .addOnFailureListener {
                body(getString(R.string.about_update_failed))
            }
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

    private fun addLink(title: String, status: String, onClick: () -> Unit) {
        val button = MaterialButton(requireContext()).apply {
            text = title
            setOnClickListener { onClick() }
        }
        binding.container.addView(button)
        body(status)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_UPDATE = 4711
    }
}
