package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.FileRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorBackupFolderTest {
    private lateinit var underTest: MonitorBackupFolder

    private val fileRepository = mock<FileRepository>()
    private val monitorUserUpdates = mock<MonitorUserUpdates>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorBackupFolder(
            fileRepository = fileRepository,
            monitorUserUpdates = monitorUserUpdates,
        )
    }

    @Test
    fun `test that current backup folder id is returned`() = runTest {
        val expected = NodeId(1L)
        fileRepository.stub {
            onBlocking { getBackupFolderId() }.thenReturn(expected)
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when user update of type backup folder is received, the new id is fetched`() =
        runTest {
            val updates = List(5) { UserChanges.MyBackupsFolder }
            val expected = NodeId(1L)
            val expectedUpdates = updates.mapIndexed { index, _ -> NodeId(index.toLong()) }

            fileRepository.stub {
                onBlocking { getBackupFolderId() }.thenReturn(
                    expected,
                    *expectedUpdates.toTypedArray()
                )
            }
            whenever(monitorUserUpdates()).thenReturn(updates.asFlow())
            underTest().test {
                assertThat(awaitItem()).isEqualTo(expected)
                expectedUpdates.forEach {
                    assertThat(awaitItem()).isEqualTo(it)
                }
                awaitComplete()
            }

        }

    @Test
    fun `test that if non backup folder user updates are emitted, no new id is fetched`() =
        runTest {
            val updates = UserChanges.values().filterNot { it == UserChanges.MyBackupsFolder }
            val expected = NodeId(1L)
            val notExpected = NodeId(2L)
            fileRepository.stub {
                onBlocking { getBackupFolderId() }.thenReturn(expected, notExpected)
            }
            whenever(monitorUserUpdates()).thenReturn(updates.asFlow())

            underTest().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

}