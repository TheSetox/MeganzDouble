package mega.privacy.android.app.di.photos

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultGetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.DefaultDownloadPreview
import mega.privacy.android.domain.usecase.DefaultDownloadThumbnail
import mega.privacy.android.domain.usecase.DefaultEnablePhotosCameraUpload
import mega.privacy.android.domain.usecase.DefaultFilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.DefaultFilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.DefaultFilterFavourite
import mega.privacy.android.domain.usecase.DefaultFilterGIF
import mega.privacy.android.domain.usecase.DefaultGetAlbums
import mega.privacy.android.domain.usecase.DefaultGetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.DefaultGetPreview
import mega.privacy.android.domain.usecase.DefaultGetThumbnail
import mega.privacy.android.domain.usecase.DefaultGetTimelinePhotos
import mega.privacy.android.domain.usecase.DefaultSetInitialCUPreferences
import mega.privacy.android.domain.usecase.DownloadPreview
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.EnablePhotosCameraUpload
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.FilterFavourite
import mega.privacy.android.domain.usecase.FilterGIF
import mega.privacy.android.domain.usecase.GetAlbums
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.GetThumbnail
import mega.privacy.android.domain.usecase.GetTimelinePhotos
import mega.privacy.android.domain.usecase.IsCameraSyncPreferenceEnabled
import mega.privacy.android.domain.usecase.SetInitialCUPreferences

@Module
@InstallIn(ViewModelComponent::class)
abstract class PhotosUseCases {

    @Binds
    abstract fun bindGetAlbums(useCase: DefaultGetAlbums): GetAlbums

    @Binds
    abstract fun bindGetThumbnail(useCase: DefaultGetThumbnail): GetThumbnail

    @Binds
    abstract fun bindDownloadThumbnail(useCase: DefaultDownloadThumbnail): DownloadThumbnail

    @Binds
    abstract fun bindDownloadPreview(useCase: DefaultDownloadPreview): DownloadPreview

    @Binds
    abstract fun bindGetPreview(useCase: DefaultGetPreview): GetPreview

    @Binds
    abstract fun bindGetTimelinePhotos(useCase: DefaultGetTimelinePhotos): GetTimelinePhotos

    @Binds
    abstract fun bindFilterCameraUploadPhotos(useCase: DefaultFilterCameraUploadPhotos): FilterCameraUploadPhotos

    @Binds
    abstract fun bindFilterCloudDrivePhotos(useCase: DefaultFilterCloudDrivePhotos): FilterCloudDrivePhotos

    @Binds
    abstract fun bindEnableCameraUpload(useCase: DefaultEnablePhotosCameraUpload): EnablePhotosCameraUpload

    @Binds
    abstract fun bindSetInitialCUPreferences(useCase: DefaultSetInitialCUPreferences): SetInitialCUPreferences

    @Binds
    abstract fun bindGetNodeListByIds(useCase: DefaultGetNodeListByIds): GetNodeListByIds

    @Binds
    abstract fun bindGetDefaultAlbumPhotos(useCase: DefaultGetDefaultAlbumPhotos): GetDefaultAlbumPhotos

    @Binds
    abstract fun bindFilterFavourite(useCase: DefaultFilterFavourite): FilterFavourite

    @Binds
    abstract fun bindFilterGIF(useCase: DefaultFilterGIF): FilterGIF

    companion object {
        @Provides
        fun providesIsCameraSyncPreferenceEnabled(settingsRepository: SettingsRepository): IsCameraSyncPreferenceEnabled =
            IsCameraSyncPreferenceEnabled(settingsRepository::isCameraSyncPreferenceEnabled)
    }
}