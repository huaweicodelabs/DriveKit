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

package com.example.drive.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.drive.R;
import com.huawei.cloud.services.drive.model.File;

import java.util.ArrayList;

public class ItemAdapter extends BaseAdapter {

    private ArrayList<File> myFileList;

    private Context mContext;

    public ItemAdapter(ArrayList<File> arrayList, Context mContext) {
        myFileList = arrayList;
        this.mContext = mContext;
    }

    public void setFileList(ArrayList<File> myFileList) {
        this.myFileList = myFileList;
    }

    @Override
    public int getCount() {
        if (myFileList != null) {
            return myFileList.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (myFileList == null) {
            return null;
        }
        if (position == -1 || position >= myFileList.size()) {
            return null;
        }
        return myFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position < 0 || position >= myFileList.size()) {
            return new TextView(mContext);
        }
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.view_file_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.icon);
            viewHolder.textView = convertView.findViewById(R.id.fileName);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        File file = myFileList.get(position);
        Drawable drawable = mContext.getDrawable(judgeFileType(file.getMimeType()));
        viewHolder.imageView.setImageDrawable(drawable);
        viewHolder.textView.setText(file.getFileName());
        return convertView;
    }

    private int judgeFileType(String mimeType) {
        if ("application/vnd.huawei-apps.folder".equals(mimeType)) {
            return R.mipmap.hidisk_icon_folder;
        }
        if (mimeType.contains("image")) {
            return R.mipmap.hidisk_icon_picture;
        }
        return R.mipmap.hidisk_icon_unknown1;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}
