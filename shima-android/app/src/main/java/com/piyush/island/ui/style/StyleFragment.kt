package com.piyush.island.ui.style

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.piyush.island.core.AppGraph
import com.piyush.island.databinding.FragmentStyleBinding
import com.piyush.island.model.CutoutSide
import com.piyush.island.model.IslandAnimation
import com.piyush.island.model.PaletteId
import com.piyush.island.model.ThemeMode
import kotlinx.coroutines.launch

/**
 * The Style tab: where the island sits, how big it is, which palette it uses and how it moves.
 */
class StyleFragment : Fragment() {

    private var _binding: FragmentStyleBinding? = null
    private val binding get() = _binding!!
    private var suppress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStyleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildThemeChips()
        buildPaletteChips()
        buildAnimationChips()
        buildCutoutChips()
        bindSliders()
        bindSwitches()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.settings.settings.collect { render() }
            }
        }
    }

    private fun buildThemeChips() {
        ThemeMode.entries.forEach { mode ->
            val chip = Chip(requireContext())
            chip.text = getString(mode.labelRes)
            chip.tag = mode
            chip.isCheckable = true
            chip.setOnClickListener {
                AppGraph.settings.setThemeMode(mode)
                render()
            }
            binding.groupTheme.addView(chip)
        }
    }

    private fun buildPaletteChips() {
        val ids = com.piyush.island.model.Palettes.selectable + PaletteId.MATERIAL_YOU
        ids.forEach { palette ->
            val chip = Chip(requireContext())
            chip.text = palette.displayName
            chip.tag = palette
            chip.isCheckable = true
            chip.setOnClickListener {
                AppGraph.settings.setPalette(palette)
                render()
            }
            binding.groupPalette.addView(chip)
        }
    }

    private fun buildAnimationChips() {
        IslandAnimation.entries.forEach { animation ->
            val chip = Chip(requireContext())
            chip.text = getString(animation.labelRes)
            chip.tag = animation
            chip.isCheckable = true
            chip.setOnClickListener {
                AppGraph.settings.updateStyle { it.copy(animation = animation) }
                render()
            }
            binding.groupAnimation.addView(chip)
        }
    }

    private fun buildCutoutChips() {
        CutoutSide.entries.forEach { side ->
            val chip = Chip(requireContext())
            chip.text = getString(side.labelRes)
            chip.tag = side
            chip.isCheckable = true
            chip.setOnClickListener {
                AppGraph.settings.updateStyle { it.copy(cutoutSide = side) }
                render()
            }
            binding.groupCutout.addView(chip)
        }
    }

    private fun bindSliders() {
        binding.sliderWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(widthDp = value.toInt()) }
        }
        binding.sliderHeight.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(heightDp = value.toInt()) }
        }
        binding.sliderRadius.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(cornerRadiusDp = value.toInt()) }
        }
        binding.sliderOffsetX.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(offsetXDp = value.toInt()) }
        }
        binding.sliderOffsetY.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(offsetYDp = value.toInt()) }
        }
        binding.sliderTransparency.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(transparency = value.toInt()) }
        }
        binding.sliderOutline.addOnChangeListener { _, value, fromUser ->
            if (fromUser && !suppress) AppGraph.settings.updateStyle { it.copy(outlineWidthDp = value.toInt()) }
        }
    }

    private fun bindSwitches() {
        binding.switchMaterialYou.setOnCheckedChangeListener { _, checked ->
            if (suppress) return@setOnCheckedChangeListener
            AppGraph.settings.setPalette(if (checked) PaletteId.MATERIAL_YOU else PaletteId.INK)
        }
        binding.switchFitCutout.setOnCheckedChangeListener { _, checked ->
            if (suppress) return@setOnCheckedChangeListener
            AppGraph.settings.updateStyle { it.copy(fitToCutout = checked) }
        }
        binding.switchHideFullscreen.setOnCheckedChangeListener { _, checked ->
            if (suppress) return@setOnCheckedChangeListener
            AppGraph.settings.updateStyle { it.copy(hideInFullscreen = checked) }
        }
        binding.switchCompanion.setOnCheckedChangeListener { _, checked ->
            if (suppress) return@setOnCheckedChangeListener
            AppGraph.settings.updateStyle { it.copy(companionEnabled = checked) }
        }
    }

    private fun render() {
        val style = AppGraph.settings.current.style
        suppress = true
        binding.sliderWidth.value = style.widthDp.toFloat().coerceIn(120f, 360f)
        binding.sliderHeight.value = style.heightDp.toFloat().coerceIn(24f, 64f)
        binding.sliderRadius.value = style.cornerRadiusDp.toFloat().coerceIn(0f, 32f)
        binding.sliderOffsetX.value = style.offsetXDp.toFloat().coerceIn(-120f, 120f)
        binding.sliderOffsetY.value = style.offsetYDp.toFloat().coerceIn(-40f, 120f)
        binding.sliderTransparency.value = style.transparency.toFloat().coerceIn(0f, 60f)
        binding.sliderOutline.value = style.outlineWidthDp.toFloat().coerceIn(0f, 6f)

        binding.switchMaterialYou.isChecked = style.palette == PaletteId.MATERIAL_YOU
        binding.switchFitCutout.isChecked = style.fitToCutout
        binding.switchHideFullscreen.isChecked = style.hideInFullscreen
        binding.switchCompanion.isChecked = style.companionEnabled

        checkChip(binding.groupTheme, style.themeMode)
        checkChip(binding.groupPalette, style.palette)
        checkChip(binding.groupAnimation, style.animation)
        checkChip(binding.groupCutout, style.cutoutSide)

        val dark = when (style.themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        binding.preview.render(style, dark)
        suppress = false
    }

    private fun checkChip(group: com.google.android.material.chip.ChipGroup, value: Any) {
        for (index in 0 until group.childCount) {
            val chip = group.getChildAt(index) as Chip
            chip.isChecked = chip.tag == value
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
