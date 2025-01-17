package mega.privacy.android.data.gateway.preferences

import kotlinx.coroutines.flow.Flow

/**
 * UI preferences gateway
 *
 */
interface UIPreferencesGateway {
    /**
     * Monitor preferred start screen
     *
     * @return preferred start screen
     */
    fun monitorPreferredStartScreen(): Flow<Int?>

    /**
     * Set preferred start screen
     *
     * @param value
     */
    suspend fun setPreferredStartScreen(value: Int)

    /**
     * Monitor start screen login timestamp
     *
     * @return start screen login timestamp
     */
    fun monitorStartScreenLoginTimestamp(): Flow<Long?>

    /**
     * Set start screen login timestamp
     *
     * @param value
     */
    suspend fun setStartScreenLoginTimestamp(value: Long)

    /**
     * Monitor do not alert about start screen
     *
     * @return true if alert should be displayed, else false
     */
    fun monitorDoNotAlertAboutStartScreen(): Flow<Boolean?>

    /**
     * Set do not alert about start screen
     *
     * @param value
     */
    suspend fun setDoNotAlertAboutStartScreen(value: Boolean)

    /**
     * Monitor hide recent activity
     *
     * @return true if recent activity should be hidden, else false
     */
    fun monitorHideRecentActivity(): Flow<Boolean?>

    /**
     * Set hide recent activity
     *
     * @param value
     */
    suspend fun setHideRecentActivity(value: Boolean)

}