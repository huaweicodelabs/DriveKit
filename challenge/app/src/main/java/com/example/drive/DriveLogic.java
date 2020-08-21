/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.drive;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.huawei.cloud.base.auth.DriveCredential;
import com.huawei.cloud.base.util.StringUtils;
import com.huawei.cloud.client.exception.DriveCode;
import com.huawei.cloud.services.drive.Drive;
import com.huawei.cloud.services.drive.model.File;
import com.huawei.cloud.services.drive.model.FileList;

import java.util.ArrayList;

public class DriveLogic extends Object {
    private final static String TAG = "DriveLogic";
    private DriveCredential driveCredential;
    private String accessToken;
    private String unionID;
    private Context context;

    private static DriveLogic instance = new DriveLogic();

    public static DriveLogic getInstance() {
        return instance;
    }

    /**
     * Initialize
     *
     * @param context context
     * @param unionID union ID from hwid
     * @param at      accessToken
     */
    public int init(Context context, String unionID, String at) {
        if (StringUtils.isNullOrEmpty(unionID) || StringUtils.isNullOrEmpty(at)) {
            return DriveCode.ERROR;
        }
        this.accessToken = at;
        this.unionID = unionID;
        this.context = context;
        DriveCredential.AccessMethod refreshAT = new DriveCredential.AccessMethod() {
            @Override
            public String refreshToken() {
                return accessToken;
            }
        };

        DriveCredential.Builder builder = new DriveCredential.Builder(this.unionID, refreshAT);
        driveCredential = builder.build().setAccessToken(this.accessToken);
        return DriveCode.SUCCESS;
    }

    /**
     * query file modes for list view
     */
    public void queryFiles(String queryString, ArrayList<File> fileArrayList) {
        // TODO: query File then use LocalBroadcastManager to broadcast com.example.drive.action.refreshList  for refreshing list
    }

    /**
     * upload file
     */
    public void uploadFile(java.io.File file) {
        // TODO: upload File
    }

    /**
     * download file
     */
    public void downloadFile(File fileMeta, String download2Path) {
        // TODO: download File
    }
}