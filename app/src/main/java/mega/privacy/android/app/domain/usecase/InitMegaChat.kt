package mega.privacy.android.app.domain.usecase

/**
 * Init megaChat API use case.
 */
fun interface InitMegaChat {

    /**
     * Invoke.
     *
     * @param session Required account session.
     */
    suspend operator fun invoke(session: String)
}