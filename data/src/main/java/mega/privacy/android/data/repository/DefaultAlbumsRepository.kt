package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AlbumsRepository
import javax.inject.Inject

/**
 * Default implementation of [AlbumsRepository]
 *
 * @property megaApiGateway MegaApiGateway
 * @property ioDispatcher CoroutineDispatcher
 * @property megaLocalStorageFacade MegaLocalStorageGateway
 * @property cacheFolderFacade CacheFolderGateway
 */
internal class DefaultAlbumsRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val megaLocalStorageFacade: MegaLocalStorageGateway,
    private val cacheFolderFacade: CacheFolderGateway,
) : AlbumsRepository {

    override suspend fun getCameraUploadFolderId(): Long? = withContext(ioDispatcher) {
        megaLocalStorageFacade.getCamSyncHandle()
    }

    override suspend fun getMediaUploadFolderId(): Long? = withContext(ioDispatcher) {
        megaLocalStorageFacade.getMegaHandleSecondaryFolder()
    }
}