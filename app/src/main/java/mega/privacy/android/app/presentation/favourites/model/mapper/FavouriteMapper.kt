package mega.privacy.android.app.presentation.favourites.model.mapper

import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteFolder
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.NodeFile
import mega.privacy.android.domain.entity.NodeFolder
import mega.privacy.android.domain.entity.NodeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import nz.mega.sdk.MegaNode

/**
 * Mapper for FavouriteInfo convert to Favourite
 */
typealias FavouriteMapper = (
    @JvmSuppressWildcards MegaNode,
    @JvmSuppressWildcards NodeInfo,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards StringUtilWrapper,
    @JvmSuppressWildcards (String) -> Int,
) -> @JvmSuppressWildcards Favourite

/**
 * Convert NodeInfo to Favourite
 * @param nodeInfo FavouriteInfo
 * @param isAvailableOffline isAvailableOffline
 * @param stringUtil StringUtilWrapper
 * @param getFileIcon getFileIcon
 * @return Favourite
 */
internal fun toFavourite(
    node: MegaNode,
    nodeInfo: NodeInfo,
    isAvailableOffline: Boolean,
    stringUtil: StringUtilWrapper,
    getFileIcon: (String) -> Int = { 0 },
) = when (nodeInfo) {
    is NodeFolder -> {
        nodeInfo.createFolder(
            node,
            getFolderInfo(nodeInfo, stringUtil),
            isAvailableOffline,
        )
    }
    is NodeFile -> {
        nodeInfo.createFile(
            node,
            getFileInfo(nodeInfo, stringUtil),
            isAvailableOffline,
            getFileIcon,
        )
    }
}


/**
 * Create favourite folder based on favourite info
 * @param node
 * @param folderInfo folder info
 * @param isAvailableOffline whether is available for offline
 * @return FavouriteFolder
 */
private fun NodeFolder.createFolder(
    node: MegaNode,
    folderInfo: String,
    isAvailableOffline: Boolean,
) = FavouriteFolder(
    handle = id,
    icon = MegaNodeUtil.getFolderIcon(node,
        DrawerItem.HOMEPAGE),
    name = name,
    label = label,
    labelColour = MegaNodeUtil.getNodeLabelColor(label),
    showLabel = label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = hasVersion,
    info = folderInfo,
    isFavourite = isFavourite,
    isExported = isExported,
    isTakenDown = isTakenDown,
    isAvailableOffline = isAvailableOffline
)

/**
 * Create favourite file based on favourite info
 * @param node
 * @param fileInfo file info
 * @param isAvailableOffline whether is available for offline
 * @param getFileIcon getFileIcon
 * @return FavouriteFile
 */
private fun NodeFile.createFile(
    node: MegaNode,
    fileInfo: String,
    isAvailableOffline: Boolean,
    getFileIcon: (String) -> Int,
) = FavouriteFile(
    handle = id,
    icon = getFileIcon(name),
    name = name,
    label = label,
    labelColour = MegaNodeUtil.getNodeLabelColor(label),
    showLabel = label != MegaNode.NODE_LBL_UNKNOWN,
    node = node,
    hasVersion = hasVersion,
    info = fileInfo,
    size = size,
    modificationTime = modificationTime,
    isFavourite = isFavourite,
    isExported = isExported,
    isTakenDown = isTakenDown,
    isAvailableOffline = isAvailableOffline,
    thumbnailPath = thumbnailPath?.takeIf { type.hasThumbnail() }
)

private fun FileTypeInfo.hasThumbnail(): Boolean = when (this) {
    is AudioFileTypeInfo -> true
    is VideoFileTypeInfo -> true
    is ImageFileTypeInfo -> true
    PdfFileTypeInfo -> true
    else -> false
}

/**
 * Needs to happen on the fragment. preferably using Android formatter:
 * android.text.format.Formatter.formatShortFileSize(activityContext, bytes) for size
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return file info
 */
private fun getFileInfo(favouriteInfo: NodeFile, stringUtil: StringUtilWrapper) =
    String.format(
        "%s · %s",
        stringUtil.getSizeString(favouriteInfo.size),
        stringUtil.formatLongDateTime(favouriteInfo.modificationTime)
    )

/**
 * Get folder info based on number of child folders and files
 * @param favouriteInfo FavouriteInfo
 * @param stringUtil StringUtilWrapper
 * @return folder info
 */
private fun getFolderInfo(favouriteInfo: NodeFolder, stringUtil: StringUtilWrapper) =
    stringUtil.getFolderInfo(favouriteInfo.numChildFolders, favouriteInfo.numChildFiles)