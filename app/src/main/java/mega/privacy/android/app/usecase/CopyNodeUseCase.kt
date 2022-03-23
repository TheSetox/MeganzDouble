package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.usecase.data.CopyRequestResult
import mega.privacy.android.app.usecase.exception.*
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.*
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for copying MegaNodes.
 *
 * @property megaApi            MegaApiAndroid instance to copy nodes.
 * @property getNodeUseCase     Required for getting MegaNodes.
 * @property moveNodeUseCase    Required for moving MegaNodes to the Rubbish Bin.
 */
class CopyNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase
) {

    /**
     * Copies a node.
     *
     * @param handle        The identifier of the MegaNode to copy.
     * @param parentHandle  The parent MegaNode where the node has to be copied.
     * @return Completable.
     */
    fun copy(handle: Long, parentHandle: Long): Completable =
        Completable.fromCallable {
            copy(
                getNodeUseCase.get(handle).blockingGetOrNull(),
                getNodeUseCase.get(parentHandle).blockingGetOrNull()
            ).blockingAwait()
        }

    /**
     * Copies a node.
     *
     * @param node          The MegaNoe to copy.
     * @param parentNode    The parent MegaNode where the node has to be copied.
     * @param newName       New name for the copied node. Null if it wants to keep the original one.
     * @return Completable.
     */
    fun copy(node: MegaNode?, parentNode: MegaNode?, newName: String? = null): Completable =
        Completable.create { emitter ->
            if (node == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val listener = OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                if (emitter.isDisposed) {
                    return@OptionalMegaRequestListenerInterface
                }

                when (error.errorCode) {
                    API_OK -> emitter.onComplete()
                    API_EBUSINESSPASTDUE -> emitter.onError(BusinessAccountOverdueException())
                    API_EOVERQUOTA -> {
                        if (megaApi.isForeignNode(parentNode.handle)) {
                            emitter.onError(ForeignNodeException())
                        } else {
                            emitter.onError(OverQuotaException())
                        }
                    }
                    API_EGOINGOVERQUOTA -> emitter.onError(PreOverQuotaException())
                    else -> emitter.onError(MegaException(error.errorCode, error.errorString))
                }
            })

            if (newName != null) {
                megaApi.copyNode(node, parentNode, newName, listener)
            } else {
                megaApi.copyNode(node, parentNode, listener)
            }

        }

    /**
     * Copies a node after resolving a name collision.
     *
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the node, false otherwise.
     * @return Single with the copy result.
     */
    fun copy(
        collisionResult: NameCollisionResult,
        rename: Boolean
    ): Single<CopyRequestResult> =
        Single.create { emitter ->
            val node = getNodeUseCase
                .get((collisionResult.nameCollision as NameCollision.Copy).nodeHandle)
                .blockingGetOrNull()

            if (node == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            val parentNode = getNodeUseCase
                .get(collisionResult.nameCollision.parentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            if (!rename) {
                moveNodeUseCase.moveToRubbishBin(collisionResult.nameCollision.collisionHandle)
                    .blockingSubscribeBy(onError = { error -> emitter.onError(error) })
            }

            if (emitter.isDisposed) {
                return@create
            }

            copy(node, parentNode, if (rename) collisionResult.renameName else null)
                .blockingSubscribeBy(
                    onError = { error ->
                        emitter.onSuccess(
                            CopyRequestResult(
                                count = 1,
                                errorCount = 1,
                                isForeignNode = error is MegaException && error.errorCode == MegaError.API_EOVERQUOTA
                            )
                        )
                    },
                    onComplete = {
                        emitter.onSuccess(
                            CopyRequestResult(
                                count = 1,
                                errorCount = 0,
                                isForeignNode = false
                            )
                        )
                    }
                )
        }

    /**
     * Copies a list of nodes after resolving name collisions.
     *
     * @param collisions    The list with the result of the name collisions.
     * @param rename        True if should rename the nodes, false otherwise.
     * @return Single with the copies result.
     */
    fun copy(
        collisions: List<NameCollisionResult>,
        rename: Boolean
    ): Single<CopyRequestResult> =
        Single.create { emitter ->
            var errorCount = 0
            var isForeignNode = false

            collisions.forEach { collision ->
                if (emitter.isDisposed) {
                    return@create
                }

                copy(collision, rename).blockingSubscribeBy(onError = { error ->
                    errorCount++

                    if (error is MegaException && error.errorCode == MegaError.API_EOVERQUOTA) {
                        isForeignNode = megaApi.isForeignNode(collision.nameCollision.parentHandle)
                    }
                })
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(
                    CopyRequestResult(
                        count = collisions.size,
                        errorCount = errorCount,
                        isForeignNode = isForeignNode
                    )
                )
            }
        }
}