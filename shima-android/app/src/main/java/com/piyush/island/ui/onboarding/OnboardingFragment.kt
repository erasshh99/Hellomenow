package com.piyush.island.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.piyush.island.R
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentOnboardingBinding
import com.piyush.island.model.ThemeMode
import com.piyush.island.MainActivity

/**
 * The first run: what Shima is, what it needs and where to start.
 */
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private var step = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonNext.setOnClickListener { next() }
        render()
    }

    private fun next() {
        when (step) {
            1 -> {
                MainActivity.from(this)?.openAccessibilitySettings()
                step = 2
                render()
            }

            2 -> {
                MainActivity.from(this)?.openNotificationListenerSettings()
                step = 3
                render()
            }

            else -> MainActivity.from(this)?.finishOnboarding()
        }
    }

    private fun render() {
        val style = AppGraph.settings.current.style
        binding.preview.render(style, style.themeMode != ThemeMode.LIGHT, expanded = step == 0)

        binding.steps.removeAllViews()
        when (step) {
            0 -> {
                binding.textTitle.text = getString(R.string.onboarding_welcome, getString(R.string.app_name))
                binding.textBody.setText(R.string.onboarding_welcome_body)
                bullet(getString(R.string.onboarding_music))
                bullet(getString(R.string.onboarding_charging))
                bullet(getString(R.string.onboarding_flashlight))
                bullet(getString(R.string.onboarding_low_battery))
                bullet(getString(R.string.onboarding_more))
            }

            1 -> {
                binding.textTitle.setText(R.string.onboarding_grant_access)
                binding.textBody.setText(R.string.perm_accessibility_body)
                bullet(getString(R.string.perm_accessibility_title))
                bullet(getString(R.string.perm_notification_title))
            }

            2 -> {
                binding.textTitle.setText(R.string.perm_notification_title)
                binding.textBody.setText(R.string.perm_notification_body)
            }

            else -> {
                binding.textTitle.setText(R.string.onboarding_ready)
                binding.textBody.setText(R.string.onboarding_welcome_body)
            }
        }
        binding.buttonNext.setText(if (step >= 3) R.string.finish else R.string.continue_label)
    }

    private fun bullet(text: String) {
        val view = TextView(requireContext()).apply {
            this.text = "• $text"
            textSize = 15f
            setPadding(0, 4, 0, 4)
        }
        binding.steps.addView(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
