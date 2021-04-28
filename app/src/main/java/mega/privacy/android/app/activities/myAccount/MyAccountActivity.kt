package mega.privacy.android.app.activities.myAccount

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.fragments.managerFragments.myAccount.MyAccountFragment
import mega.privacy.android.app.utils.AlertsAndWarnings.dismissAlertDialogIfShown
import mega.privacy.android.app.utils.AlertsAndWarnings.isAlertDialogShown
import mega.privacy.android.app.utils.MenuUtils.toggleAllMenuItemsVisibility
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.isOnline

class MyAccountActivity : PasscodeActivity() {

    companion object {
        private const val KILL_SESSIONS_SHOWN = "KILL_SESSIONS_SHOWN"
    }

    private val viewModel: MyAccountViewModel by viewModels()

    private var menu: Menu? = null

    private var killSessionsConfirmationDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, MyAccountFragment())
                .commit()
        } else {
            if (savedInstanceState.getBoolean(KILL_SESSIONS_SHOWN, false)) {
                showConfirmationKillSessions()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KILL_SESSIONS_SHOWN, isAlertDialogShown(killSessionsConfirmationDialog))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        dismissAlertDialogIfShown(killSessionsConfirmationDialog)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (viewModel.isMyAccountFragment()) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_kill_all_sessions -> showConfirmationKillSessions()
            R.id.action_change_pass -> {
            }
            R.id.action_export_MK -> {
            }
            R.id.action_refresh -> {
            }
            R.id.action_upgrade_account -> {
            }
            R.id.action_cancel_subscriptions -> {
            }
            R.id.action_logout -> {
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_my_account, menu)
        this.menu = menu

        refreshMenuOptionsVisibility()

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets the right Toolbar options depending on current situation.
     */
    private fun refreshMenuOptionsVisibility() {
        val menu = this.menu ?: return

        if (!isOnline(this)) {
            menu.toggleAllMenuItemsVisibility(false)
            return
        }

        if (viewModel.isMyAccountFragment()) {
            menu.toggleAllMenuItemsVisibility(true)

            if (viewModel.thereIsNoSubscription()) {
                menu.findItem(R.id.action_cancel_subscriptions).isVisible = false
            }
        } else {
            menu.toggleAllMenuItemsVisibility(false)
            menu.findItem(R.id.action_refresh).isVisible = true
            menu.findItem(R.id.action_logout).isVisible = true
        }
    }

    private fun showConfirmationKillSessions() {
        if (isAlertDialogShown(killSessionsConfirmationDialog)) {
            return
        }

        killSessionsConfirmationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(StringResourcesUtils.getString(R.string.confirmation_close_sessions_title))
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_close_sessions_text))
            .setPositiveButton(StringResourcesUtils.getString(R.string.contact_accept)) { _, _ ->
                viewModel.killSessions()
            }.setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
            .show()
    }
}