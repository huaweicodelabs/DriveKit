/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.huawei.drivekitkotlindemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.huawei.cloud.base.auth.DriveCredential
import com.huawei.cloud.base.auth.DriveCredential.AccessMethod
import com.huawei.cloud.base.http.FileContent
import com.huawei.cloud.base.util.StringUtils
import com.huawei.cloud.client.exception.DriveCode
import com.huawei.cloud.services.drive.Drive
import com.huawei.cloud.services.drive.DriveScopes
import com.huawei.cloud.services.drive.model.Comment
import com.huawei.cloud.services.drive.model.File
import com.huawei.cloud.services.drive.model.FileList
import com.huawei.cloud.services.drive.model.Reply
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.hwid.HuaweiIdAuthAPIManager
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var mCredential: DriveCredential? = null
    private var accessToken: String? = null
    private var unionId: String? = null
    private var directoryCreated: File? = null
    private var fileUploaded: File? = null
    private var fileSearched: File? = null
    private var mComment: Comment? = null
    private var mReply: Reply? = null


    companion object {
        private val MIME_TYPE_MAP: MutableMap<String, String> = HashMap()
        private const val REQUEST_SIGN_IN_LOGIN = 1002
        private const val TAG = "MainActivity"

        init {
            MIME_TYPE_MAP.apply {
                put(".doc", "application/msword")
                put(".jpg", "image/jpeg")
                put(".mp3", "audio/x-mpeg")
                put(".mp4", "video/mp4")
                put(".pdf", "application/pdf")
                put(".png", "image/png")
                put(".txt", "text/plain")
            }
        }
    }

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private val refreshAT = AccessMethod {
        /**
         * Simplified code snippet for demonstration purposes. For the complete code snippet,
         * please go to Client Development > Obtaining Authentication Information > Store Authentication Information
         * in the HUAWEI Drive Kit Development Guide.
         **/
        return@AccessMethod accessToken
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar.title = R.string.drive_application.toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS_STORAGE, 1)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Exceptional process for obtaining account information. Obtain and save the related accessToken and unionID using this function.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult, requestCode = $requestCode, resultCode = $resultCode")
        when (requestCode) {
            REQUEST_SIGN_IN_LOGIN -> {
                val authHuaweiIdTask =
                    HuaweiIdAuthManager.parseAuthResultFromIntent(data)
                if (authHuaweiIdTask.isSuccessful) {
                    val huaweiAccount = authHuaweiIdTask.result
                    accessToken = huaweiAccount.accessToken
                    unionId = huaweiAccount.unionId
                    val returnCode = init(unionId, accessToken, refreshAT)
                    if (DriveCode.SUCCESS == returnCode) {
                        showTips("login ok")
                    } else if (DriveCode.SERVICE_URL_NOT_ENABLED == returnCode) {
                        showTips("drive is not enabled")
                    } else {
                        showTips("login error")
                    }
                } else {
                    Log.d(
                        TAG,
                        "onActivityResult, signIn failed: " + (authHuaweiIdTask.exception as ApiException).statusCode
                    )
                    Toast.makeText(
                        applicationContext,
                        "onActivityResult, signIn failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * （unionId，countrycode，accessToken）drive。
     * accessTokenAccessMethod,accessToken。
     *
     * @param unionID   unionID from HwID
     * @param at        access token
     * @param refreshAT a callback to refresh AT
     */
    private fun init(unionID: String?, at: String?, refreshAT: AccessMethod?): Int {
        return if (StringUtils.isNullOrEmpty(unionID) || StringUtils.isNullOrEmpty(at)) {
            DriveCode.ERROR
        } else {
            val builder = DriveCredential.Builder(unionID, refreshAT)
            mCredential = builder.build().setAccessToken(at)
            DriveCode.SUCCESS
        }
    }

    private fun showTips(toastText: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, toastText, Toast.LENGTH_LONG).show()
            textView.text = toastText
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonLogin -> driveLogin()
            R.id.buttonUploadFiles -> uploadFiles()
            R.id.buttonQueryFiles -> queryFiles()
            R.id.buttonDownloadFiles -> downloadFiles()
            R.id.buttonCreateComment -> createComment()
            R.id.buttonQueryComment -> queryComment()
            R.id.buttonCreateReply -> createReply()
            R.id.buttonQueryReply -> queryReply()
            else -> {
            }
        }
    }

    //Drive Login starts
    private fun driveLogin() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        val scopeList: MutableList<Scope> = ArrayList()

        scopeList.apply {
            add(Scope(DriveScopes.SCOPE_DRIVE))
            add(Scope(DriveScopes.SCOPE_DRIVE_READONLY))
            add(Scope(DriveScopes.SCOPE_DRIVE_FILE))
            add(Scope(DriveScopes.SCOPE_DRIVE_METADATA))
            add(Scope(DriveScopes.SCOPE_DRIVE_METADATA_READONLY))
            add(Scope(DriveScopes.SCOPE_DRIVE_APPDATA))
            add(HuaweiIdAuthAPIManager.HUAWEIID_BASE_SCOPE)
        }

        val authParams = HuaweiIdAuthParamsHelper(
            HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM
        )
            .setAccessToken()
            .setIdToken()
            .setScopeList(scopeList)
            .createParams()
        val client = HuaweiIdAuthManager.getService(this, authParams)
        startActivityForResult(client.signInIntent, REQUEST_SIGN_IN_LOGIN)
    }

    //Function to Upload files
    @SuppressLint("SdCardPath")
    private fun uploadFiles() {
        GlobalScope.launch {
            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                }
                if (StringUtils.isNullOrEmpty(
                        uploadFileName.text.toString()
                    )
                ) {
                    showTips("Please input upload file name above.")
                    return@launch
                }
                val fileObject = java.io.File("/sdcard/" + uploadFileName.text)
                if (!fileObject.exists()) {
                    showTips("The input file does not exit.")
                    return@launch
                }
                val appProperties: MutableMap<String, String> =
                    HashMap()
                appProperties["appProperties"] = "property"
                // create somepath directory
                File().setFileName("somepath" + System.currentTimeMillis())
                    .setMimeType("application/vnd.huawei-apps.folder").appSettings =
                    appProperties
                directoryCreated = buildDrive()?.files()?.create(File())?.execute()
                // create test.jpg on cloud
                val mimeType = mimeType(fileObject)
                val content = File()
                    .setFileName(fileObject.name)
                    .setMimeType(mimeType)
                    .setParentFolder(listOf(directoryCreated?.id))
                fileUploaded = buildDrive()?.files()
                    ?.create(content, FileContent(mimeType, fileObject))
                    ?.setFields("*")
                    ?.execute()
                showTips("upload success")
            } catch (ex: Exception) {
                Log.d(TAG, "upload", ex)
                showTips("upload error $ex")
            }

        }
    }

    //Function to QueryFiles
    private fun queryFiles() {
        GlobalScope.launch {
            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                }
                if (StringUtils.isNullOrEmpty(
                        searchFileName.text.toString()
                    )
                ) {
                    showTips("please input file name above.")
                    return@launch
                } else {
                    val queryFile =
                        "fileName = '" + searchFileName.text + "' and mimeType != 'application/vnd.huawei-apps.folder'"
                    val request = buildDrive()?.files()?.list()
                    var files: FileList?
                    while (true) {
                        files = request
                            ?.setQueryParam(queryFile)
                            ?.setPageSize(10)
                            ?.setOrderBy("fileName")
                            ?.setFields("category,nextCursor,files/id,files/fileName,files/size")
                            ?.execute()
                        if (files == null || files.files.size > 0) {
                            break
                        }
                        if (!StringUtils.isNullOrEmpty(files.nextCursor)) {
                            request?.cursor = files.nextCursor
                        } else {
                            break
                        }
                    }
                    var text: String
                    if (files != null && files.files.size > 0) {
                        fileSearched = files.files[0]
                        text = fileSearched.toString()
                    } else {
                        text = "empty"
                    }
                    val finalText = text
                    runOnUiThread { queryResult.text = finalText }
                    showTips("query ok")
                }
            } catch (ex: Exception) {
                Log.d(TAG, "query", ex)
                showTips("query error $ex")
            }

        }
    }

    //function to Download files
    private fun downloadFiles() {
        GlobalScope.launch {

            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                } else if (fileSearched == null) {
                    showTips("please click 'QUERY FILE'.")
                    return@launch
                } else {
                    val content = File()
                    val request = buildDrive()?.files()?.get(fileSearched?.id)
                    content.setFileName(fileSearched?.fileName).id = fileSearched?.id
                    val downloader = request?.mediaHttpDownloader
                    fileSearched?.getSize()?.minus(1)?.let {
                        downloader?.setContentRange(
                            0, it
                        )
                    }
                    val filePath =
                        "/storage/emulated/0/Huawei/Drive/DownLoad/Demo_" + fileSearched?.fileName
                    request?.executeContentAndDownloadTo(
                        FileOutputStream(
                            java.io.File(
                                filePath
                            )
                        )
                    )
                    showTips("download to $filePath")
                }
            } catch (ex: Exception) {
                Log.d(TAG, "download", ex)
                showTips("download error $ex")
            }
        }

    }

    //Function to CreateComment
    private fun createComment() {
        GlobalScope.launch {
            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                } else if (fileSearched == null) {
                    showTips("please click 'QUERY FILE'.")
                    return@launch
                } else if (StringUtils.isNullOrEmpty(
                        commentText.text.toString()
                    )
                ) {
                    showTips("please input comment above.")
                    return@launch
                } else {
                    val comment = Comment()
                    comment.description = commentText.text.toString()
                    mComment = buildDrive()?.comments()
                        ?.create(fileSearched?.id, comment)
                        ?.setFields("*")
                        ?.execute()
                    if (mComment != null && mComment?.getId() != null) {
                        Log.i(TAG, "Add comment success")
                        showTips("Add comment success")
                    } else {
                        Log.e(TAG, "Add comment failed")
                        showTips("Add comment failed")
                    }
                }
            } catch (ex: Exception) {
                Log.d(TAG, "Add comment", ex)
                showTips("Add comment error")
            }

        }
    }

    //Function to QueryComment
    private fun queryComment() {
        GlobalScope.launch {
            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                } else if (fileSearched == null) {
                    showTips("please click 'QUERY FILE'.")
                    return@launch
                } else {
                    val response = buildDrive()?.comments()
                        ?.list(fileSearched?.id)
                        ?.setFields("comments/id,comments/description,comments/replies/description")
                        ?.execute()
                    val text = response?.comments.toString()
                    runOnUiThread { commentList.text = text }
                }
            } catch (ex: Exception) {
                Log.d(TAG, "query comment", ex)
                showTips("query comment error")
            }
        }

    }

    //Function to CreateReply
    private fun createReply() {
        GlobalScope.launch {
            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                } else if (fileSearched == null) {
                    showTips("please click 'QUERY FILE'.")
                    return@launch
                } else if (mComment == null) {
                    showTips("please click 'COMMENT THE FILE'.")
                    return@launch
                } else if (StringUtils.isNullOrEmpty(replyText.text.toString())) {
                    showTips("please input comment above.")
                    return@launch
                } else {
                    Reply().description = replyText.text.toString()
                    mReply = buildDrive()?.replies()
                        ?.create(fileSearched?.id, mComment?.id, Reply())
                        ?.setFields("*")
                        ?.execute()
                    if (mReply != null && mReply?.getId() != null) {
                        Log.i(TAG, "Add reply success")
                        showTips("Add reply success")
                    } else {
                        Log.e(TAG, "Add reply failed")
                        showTips("Add reply failed")
                    }
                }
            } catch (ex: Exception) {
                Log.d(TAG, "Add reply", ex)
                showTips("Add reply error")
            }
        }
    }

    //Function to QueryReply
    private fun queryReply() {
        GlobalScope.launch {
            try {
                if (accessToken == null) {
                    showTips("please click 'Login'.")
                    return@launch
                } else if (fileSearched == null) {
                    showTips("please click 'QUERY FILE'.")
                    return@launch
                } else if (mComment == null) {
                    showTips("please click 'COMMENT THE FILE'.")
                    return@launch
                } else {
                    val response = buildDrive()?.replies()
                        ?.list(fileSearched?.id, mComment?.id)
                        ?.setFields("replies/id,replies/description")
                        ?.execute()
                    val text = response?.replies.toString()
                    runOnUiThread { replyList.text = text }
                }
            } catch (ex: Exception) {
                Log.d(TAG, "query reply", ex)
                showTips("query reply error")
            }
        }
    }

    private fun buildDrive() = Drive.Builder(mCredential, this).build()


    private fun mimeType(file: java.io.File?): String? {
        if (file != null && file.exists() && file.name.contains(".")) {
            val fileName = file.name
            val suffix = fileName.substring(fileName.lastIndexOf("."))
            if (MIME_TYPE_MAP.keys.contains(suffix)) {
                return MIME_TYPE_MAP[suffix]
            }
        }
        return "*/*"
    }
}
