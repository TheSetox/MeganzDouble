package mega.privacy.android.app.presentation.settings.reportissue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.ThemeMode
import mega.privacy.android.app.domain.usecase.GetThemeMode
import mega.privacy.android.app.presentation.settings.reportissue.model.SubmitIssueResult
import mega.privacy.android.app.presentation.settings.reportissue.view.ReportIssueView
import mega.privacy.android.app.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * Report issue fragment
 *
 */
@AndroidEntryPoint
class ReportIssueFragment : Fragment() {

    private val viewModel: ReportIssueViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AndroidTheme(mode = getThemeMode().collectAsState(initial = ThemeMode.System).value) {
                ReportIssueView(viewModel)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val valid = viewModel.state.value.canSubmit
        menu.findItem(R.id.menu_report_issue_submit)?.let {
            it.isEnabled = valid
            it.icon.alpha = if (valid) 255 else 125
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.report_issue_submit_action, menu)
        menu.findItem(R.id.menu_report_issue_submit)?.icon?.setTint(requireContext().getColor(R.color.design_default_color_secondary))
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_report_issue_submit -> {
                viewModel.submit()
                true
            }
            android.R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setBackPressHandler()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state
                    .distinctUntilChanged { old, new -> old.canSubmit == new.canSubmit && old.result == new.result }
                    .collect { state ->
                        if (state.result != null) {
                            finishWithResult(state.result)
                        } else {
                            requireActivity().invalidateOptionsMenu()
                        }
                    }
            }
        }
    }

    private val onBackPress = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!viewModel.interceptNavigation()) {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    private fun setBackPressHandler() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, onBackPress)
    }

    private fun finishWithResult(result: SubmitIssueResult) {
        tag?.let {
            setFragmentResult(it,
                bundleOf(ReportIssueFragment::class.java.name to result.getResultString(
                    requireContext())))
        }
        parentFragmentManager.popBackStack()
    }

    /**
     * Report issue view
     *
     * @param viewModel
     */
    @Composable
    fun ReportIssueView(
        viewModel: ReportIssueViewModel = viewModel(),
    ) {
        val uiState by viewModel.state.collectAsState()
        ReportIssueView(
            state = uiState,
            onDescriptionChanged = viewModel::setDescription,
            onIncludeLogsChanged = viewModel::setIncludeLogsEnabled,
            cancelUpload = viewModel::cancelUpload,
            onNavigationCancelled = viewModel::navigationCancelled,
            onDiscard = {
                onBackPress.isEnabled = false
                requireActivity().onBackPressed()
            },
        )
    }

}