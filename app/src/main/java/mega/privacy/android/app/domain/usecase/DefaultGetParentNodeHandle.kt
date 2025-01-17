package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import javax.inject.Inject

/**
 * Default get the parent node handle of a node
 *
 * @param getNodeByHandle
 * @param filesRepository
 */
class DefaultGetParentNodeHandle @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val filesRepository: FilesRepository,
) : GetParentNodeHandle {

    override suspend fun invoke(handle: Long): Long? {
        return getNodeByHandle(handle)?.let {
            filesRepository.getParentNode(it)?.handle
        }
    }
}