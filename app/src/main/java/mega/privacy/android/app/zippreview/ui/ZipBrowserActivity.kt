package mega.privacy.android.app.zippreview.ui

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.dragger.DragToExitSupport.*
import mega.privacy.android.app.databinding.ActivityZipBrowserBinding
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.zippreview.viewmodel.ZipBrowserViewModel
import mega.privacy.android.app.zippreview.viewmodel.ZipBrowserViewModel.TypeItemClickResult.*
import mega.privacy.android.app.zippreview.domain.ZipFileType
import nz.mega.sdk.MegaApiJava
import java.io.File
import java.util.*

class ZipBrowserActivity : PasscodeActivity() {
    companion object {
        const val EXTRA_PATH_ZIP = "PATH_ZIP"
        const val EXTRA_HANDLE_ZIP = "HANDLE_ZIP"
        const val EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN"
        const val ACTION_OPEN_ZIP_FILE = "OPEN_ZIP_FILE"
        const val MEGA_DOWNLOADS = "MEGA Downloads/"

        const val URI_FILE_PROVIDER = "mega.privacy.android.app.providers.fileprovider"
        const val TYPE_AUDIO = "audio/*"

        const val RATIO_RECYCLE_VIEW = 85.0f / 548
    }

    private lateinit var zipBrowserBinding: ActivityZipBrowserBinding
    private var actionBar: ActionBar? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var zipAdapter: ZipListAdapter

    private lateinit var zipFullPath: String
    private lateinit var unZipRootPath: String

    private lateinit var unZipWaitingDialog: AlertDialog

    private val zipBrowserViewModel by viewModels<ZipBrowserViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
        initView()
        setupViewModel()
        zipAdapter = ZipListAdapter { zipInfoUIO, position ->
            zipBrowserViewModel.onZipFileClicked(zipInfoUIO, position)
        }
        recyclerView.adapter = zipAdapter
    }

    private fun initData() {
        intent.extras?.run {
            //Get the zip file path
            zipFullPath = getString(EXTRA_PATH_ZIP) ?: ""
            unZipRootPath = "${zipFullPath.subSequence(0, zipFullPath.lastIndexOf("/") + 1)}"
        }
    }

    private fun initView() {
        zipBrowserBinding = ActivityZipBrowserBinding.inflate(layoutInflater)
        setContentView(zipBrowserBinding.root)

        val toolbar = zipBrowserBinding.toolbar
        toolbar.also {
            it.visibility = View.VISIBLE
            setSupportActionBar(it)
        }
        actionBar = supportActionBar
        actionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = (StringResourcesUtils.getString(R.string.zip_browser_activity).toUpperCase(
                Locale.getDefault()
            ))
        }

        recyclerView = zipBrowserBinding.zipListViewBrowser.also {
            it.setPadding(0, 0, 0, recycleViewHeight())
            it.clipToPadding = false
            it.addItemDecoration(SimpleDividerItemDecoration(this))
            it.layoutManager = LinearLayoutManager(this)
            it.setHasFixedSize(true)
            it.itemAnimator = DefaultItemAnimator()
        }
    }

    private fun setupViewModel() {
        zipBrowserViewModel.apply {
            title.observe(this@ZipBrowserActivity, { title ->
                actionBar?.title = title
            })
            zipInfoList.observe(this@ZipBrowserActivity, { zipInfoList ->
                zipAdapter.submitList(zipInfoList)
            })
            showProgressDialog.observe(this@ZipBrowserActivity, { showProgressDialog ->
                if (showProgressDialog) {
                    showProgressDialog()
                } else if (unZipWaitingDialog.isShowing) {
                    unZipWaitingDialog.dismiss()
                }
            })
            showAlert.observe(this@ZipBrowserActivity, { isShowAlert ->
                if (isShowAlert) {
                    showAlert()
                }
            })
            openFile.observe(this@ZipBrowserActivity, { openFile ->
                openFile(openFile.second, openFile.first)
            })
            //Open current zip file content
            viewModelInit(
                zipFullPath,
                resources.getString(R.string.transfer_unknown),
                unZipRootPath
            )
        }
    }

    override fun onBackPressed() {
        if (zipBrowserViewModel.backOnPress()) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        logDebug("OnOptionsItemSelected")
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return true
    }

    private fun showSnackBar(message: String) {
        showSnackbar(zipBrowserBinding.zipLayout, message)
    }

    private fun recycleViewHeight(): Int {
        return (RATIO_RECYCLE_VIEW * getScreenHeight()).toInt() //Based on Eduardo's measurements
    }

    private fun showProgressDialog() {
        if (!::unZipWaitingDialog.isInitialized) {
            unZipWaitingDialog = createProgressDialog(
                this@ZipBrowserActivity,
                getString(R.string.unzipping_process)
            )
            unZipWaitingDialog.show()
        }
    }

    private fun showAlert() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setMessage(StringResourcesUtils.getString(R.string.error_fail_to_open_file_general))
            .setPositiveButton(
                StringResourcesUtils.getString(R.string.general_ok)
                    .toUpperCase(Locale.getDefault())
            ) { _: DialogInterface?, _: Int -> }
            .show()
    }

    private fun openFile(zipInfoBO: ZipInfoUIO, position: Int) {
        if (zipInfoBO.fileType == ZipFileType.ZIP) {
            openZipFile(zipInfoBO)
        } else {
            val file = File("$unZipRootPath${zipInfoBO.zipFileName}")
            MimeTypeList.typeForName(file.name).apply {
                when {
                    isImage ->
                        imageFileOpen(position, file)
                    isVideoReproducible || isAudio ->
                        mediaFileOpen(file, position)
                    isPdf ->
                        pdfFileOpen(file, position)
                    isOpenableTextFile(file.length()) -> {
                        startActivity(
                            Intent(
                                this@ZipBrowserActivity,
                                TextEditorActivity::class.java
                            ).putExtra(INTENT_EXTRA_KEY_FILE_NAME, file.name)
                                .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
                                .putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                        )
                    }
                    else -> otherFileOpen(file)
                }
            }
        }
    }

    private fun openZipFile(zipInfoUIO: ZipInfoUIO) {
        val intentZip = Intent(
            this@ZipBrowserActivity,
            ZipBrowserActivity::class.java
        )
        intentZip.putExtra(EXTRA_PATH_ZIP, "${unZipRootPath}${zipInfoUIO.zipFileName}")
        startActivity(intentZip)
    }

    private fun imageFileOpen(position: Int, file: File) {
        logDebug("isImage")
        val intent =
            Intent(this@ZipBrowserActivity, FullScreenImageViewerLollipop::class.java)
        intent.apply {
            putExtra(INTENT_EXTRA_KEY_POSITION, position)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, false)
            putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
            putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, MegaApiJava.ORDER_DEFAULT_ASC)
        }
        DragToExitSupport.putThumbnailLocation(
            intent,
            recyclerView,
            position,
            VIEWER_FROM_ZIP_BROWSER,
            zipAdapter
        )
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun MimeTypeList.mediaFileOpen(file: File, position: Int) {
        logDebug("Video file")
        val mediaIntent: Intent
        val internalIntent: Boolean
        var opusFile = false
        if (isVideoNotSupported || isAudioNotSupported) {
            mediaIntent = Intent(Intent.ACTION_VIEW)
            internalIntent = false
            val array = file.name.split("\\.")
            if (array.size > 1 && array.last() == "opus") {
                opusFile = true
            }
        } else {
            internalIntent = true
            mediaIntent = Util.getMediaIntent(this@ZipBrowserActivity, file.name)
        }
        mediaIntent.apply {
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, file.name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, file.name.hashCode().toLong())
            putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_POSITION, position)
            putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
            putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, MegaApiJava.ORDER_DEFAULT_ASC)
            DragToExitSupport.putThumbnailLocation(
                mediaIntent,
                recyclerView,
                position,
                VIEWER_FROM_ZIP_BROWSER,
                zipAdapter
            )
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        getExternalFilesDir(null)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && file.absolutePath.contains(path)) {
                mediaIntent.setDataAndType(
                    FileProvider.getUriForFile(
                        this@ZipBrowserActivity,
                        URI_FILE_PROVIDER,
                        file
                    ), type
                )
            } else {
                mediaIntent.setDataAndType(Uri.fromFile(file), type)
            }
        }
        if (opusFile) {
            mediaIntent.setDataAndType(mediaIntent.data, TYPE_AUDIO)
        }
        if (internalIntent) {
            startActivity(mediaIntent)
        } else {
            if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, mediaIntent)) {
                startActivity(mediaIntent)
            } else {
                showSnackBar(resources.getString(R.string.intent_not_available))

                val intentShare = Intent(Intent.ACTION_SEND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intentShare.setDataAndType(
                        FileProvider.getUriForFile(
                            this@ZipBrowserActivity,
                            URI_FILE_PROVIDER,
                            file
                        ), type
                    )
                } else {
                    intentShare.setDataAndType(Uri.fromFile(file), type)
                }
                intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, intentShare)) {
                    logDebug("Call to startActivity(intentShare)")
                    startActivity(intentShare)
                }
            }
        }
        overridePendingTransition(0, 0)
    }

    private fun MimeTypeList.pdfFileOpen(file: File, position: Int) {
        logDebug("Pdf file")
        val pdfIntent =
            Intent(this@ZipBrowserActivity, PdfViewerActivityLollipop::class.java)
        pdfIntent.apply {
            putExtra(INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
        }
        DragToExitSupport.putThumbnailLocation(
            pdfIntent,
            recyclerView,
            position,
            VIEWER_FROM_ZIP_BROWSER,
            zipAdapter
        )
        getExternalFilesDir(null)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && file.absolutePath.contains(path)) {
                pdfIntent.setDataAndType(
                    FileProvider.getUriForFile(
                        this@ZipBrowserActivity,
                        URI_FILE_PROVIDER,
                        file
                    ), type
                )
            } else {
                pdfIntent.setDataAndType(Uri.fromFile(file), type)
            }
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(pdfIntent)
            overridePendingTransition(0, 0)
        }
    }

    private fun MimeTypeList.otherFileOpen(file: File) {
        logDebug("NOT Image, video, audio or pdf")
        val viewIntent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            viewIntent.setDataAndType(
                FileProvider.getUriForFile(
                    this@ZipBrowserActivity,
                    URI_FILE_PROVIDER,
                    file
                ), type
            )
        } else {
            viewIntent.setDataAndType(Uri.fromFile(file), type)
        }
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, viewIntent)) {
            startActivity(viewIntent)
        } else {
            val intentShare = Intent(Intent.ACTION_SEND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intentShare.setDataAndType(
                    FileProvider.getUriForFile(
                        this@ZipBrowserActivity,
                        URI_FILE_PROVIDER,
                        file
                    ), type
                )
            } else {
                intentShare.setDataAndType(Uri.fromFile(file), type)
            }
            intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, intentShare)) {
                logDebug("Call to startActivity(intentShare)")
                startActivity(intentShare)
            }
            val toastMessage =
                "${getString(R.string.general_already_downloaded)}:${file.absolutePath}"
            Toast.makeText(this@ZipBrowserActivity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }
}
