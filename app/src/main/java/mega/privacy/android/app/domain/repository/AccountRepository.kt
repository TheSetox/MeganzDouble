package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.UserAccount
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Account repository
 */
interface AccountRepository {
    /**
     * Get user account
     *
     * @return the user account for the current user
     */
    fun getUserAccount(): UserAccount

    /**
     * Is account data stale
     *
     * @return true if account data is stale. else false
     */
    fun isAccountDataStale(): Boolean

    /**
     * Request account
     * Sends a request to update account data asynchronously
     */
    fun requestAccount()

    /**
     * Get root node
     *
     * This method requires some refactoring as MegaNode is not a domain entity and thus violates the architecture
     *
     * @return the mega root node.
     */
    fun getRootNode(): MegaNode?

    /**
     * Is multi factor auth available
     *
     * @return true if multi-factor auth is available for the current user, else false
     */
    fun isMultiFactorAuthAvailable(): Boolean

    /**
     * Is multi factor auth enabled
     *
     * @return true if multi-factor auth is enabled for the current user, else false
     */
    suspend fun isMultiFactorAuthEnabled(): Boolean

    /**
     * Monitor multi factor auth changes
     *
     * @return a flow that emits changes to the multi-factor auth enabled state
     */
    fun monitorMultiFactorAuthChanges(): Flow<Boolean>

    /**
     * Request delete account link
     *
     * Sends a delete account link to the user's email address
     *
     */
    suspend fun requestDeleteAccountLink()
}