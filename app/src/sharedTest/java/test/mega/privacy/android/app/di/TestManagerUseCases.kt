package test.mega.privacy.android.app.di

import dagger.Provides
import dagger.Module
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.manager.ManagerUseCases
import mega.privacy.android.app.domain.usecase.GetBrowserChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinChildrenNode
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import nz.mega.sdk.MegaNode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [ManagerUseCases::class],
    components = [ViewModelComponent::class]
)
@Module
object TestManagerUseCases {

    @Provides
    fun provideMonitorGlobalUpdates() = mock<MonitorGlobalUpdates>()

    @Provides
    fun provideMonitorNodeUpdates() = mock<MonitorNodeUpdates> {
        on { run { invoke() } }.thenReturn(flowOf(any()))
    }

    @Provides
    fun provideRubbishBinChildrenNode() = mock<GetRubbishBinChildrenNode> {
        on { runBlocking { invoke(0) } }.thenReturn(emptyList())
    }

    @Provides
    fun provideBrowserChildrenNode() = mock<GetBrowserChildrenNode> {
        on { runBlocking { invoke(0) } }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetRootFolder() = mock<GetRootFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetRubbishBinFolder() = mock<GetRubbishBinFolder> {
        on { runBlocking { invoke() } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetChildrenNode() = mock<GetChildrenNode> {
        on { runBlocking { invoke(any(), null) } }.thenReturn(emptyList())
    }

    @Provides
    fun provideGetNodeByHandle() = mock<GetNodeByHandle> {
        on { runBlocking { invoke(any()) } }.thenReturn(MegaNode())
    }

    @Provides
    fun provideGetNumUnreadUserAlerts() = mock<GetNumUnreadUserAlerts> {
        on { runBlocking { invoke() } }.thenReturn(0)
    }
}