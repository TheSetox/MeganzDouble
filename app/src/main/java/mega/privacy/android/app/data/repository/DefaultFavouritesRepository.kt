package mega.privacy.android.app.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.MonitorNodeChangeFacade
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.MegaNodeUtil.getThumbnailFileName
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.FavouriteFolderInfoMapper
import mega.privacy.android.data.mapper.NodeInfoMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.FavouriteFolderInfo
import mega.privacy.android.domain.entity.NodeInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FavouritesRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * The repository implementation class regarding favourites
 * @param megaApiGateway MegaApiGateway
 * @param ioDispatcher IODispatcher
 * @param monitorNodeChangeFacade MonitorNodeChangeFacade
 */
class DefaultFavouritesRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorNodeChangeFacade: MonitorNodeChangeFacade,
    private val nodeInfoMapper: NodeInfoMapper,
    private val favouriteFolderInfoMapper: FavouriteFolderInfoMapper,
    private val cacheFolder: CacheFolderGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
) : FavouritesRepository {

    override suspend fun getAllFavorites(): List<NodeInfo> =
        withContext(ioDispatcher) {
            val handleList = suspendCoroutine<MegaHandleList> { continuation ->
                megaApiGateway.getFavourites(
                    node = null,
                    count = 0,
                    listener = OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(request.megaHandleList))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
            mapNodesToFavouriteInfo(handleList.getNodes())
        }

    override suspend fun getChildren(parentHandle: Long): FavouriteFolderInfo? =
        withContext(ioDispatcher) {
            megaApiGateway.getMegaNodeByHandle(parentHandle)?.let { parentNode ->
                favouriteFolderInfoMapper(
                    parentNode,
                    mapNodesToFavouriteInfo(megaApiGateway.getChildrenByNode(parentNode)),
                    parentHandle
                )
            }
        }

    override fun monitorNodeChange(): Flow<Boolean> = monitorNodeChangeFacade.getEvents()

    override suspend fun removeFavourites(handles: List<Long>) {
        withContext(ioDispatcher) {
            handles.map { handle ->
                megaApiGateway.getMegaNodeByHandle(handle)
            }.forEach { megaNode ->
                megaApiGateway.setNodeFavourite(megaNode, false)
            }
        }
    }

    /**
     * Get node from MegaHandleList
     * @return List<MegaNode>
     */
    private suspend fun MegaHandleList.getNodes() = (0..size())
        .map { this[it] }
        .mapNotNull { megaApiGateway.getMegaNodeByHandle(it) }

    /**
     * Convert the MegaNode list to FavouriteInfo list
     * @param nodes List<MegaNode>
     * @return FavouriteInfo list
     */
    private suspend fun mapNodesToFavouriteInfo(nodes: List<MegaNode>) =
        nodes.map { megaNode ->
            nodeInfoMapper(
                megaNode,
                ::getThumbnailCacheFilePath,
                megaApiGateway::hasVersion,
                megaApiGateway::getNumChildFolders,
                megaApiGateway::getNumChildFiles,
                fileTypeInfoMapper,
                megaApiGateway::isPendingShare,
                megaApiGateway::isInRubbish,
            )
        }

    /**
     * Get the thumbnail cache file path
     * @param megaNode MegaNode
     * @return thumbnail cache file path
     */
    private fun getThumbnailCacheFilePath(megaNode: MegaNode) =
        cacheFolder.getCacheFolder(CacheFolderManager.THUMBNAIL_FOLDER)?.let { thumbnail ->
            "$thumbnail${File.separator}${megaNode.getThumbnailFileName()}"
        }
}