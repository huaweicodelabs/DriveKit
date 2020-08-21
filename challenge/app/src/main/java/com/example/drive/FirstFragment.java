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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.drive.adapter.ItemAdapter;
import com.huawei.cloud.base.util.DateTime;
import com.huawei.cloud.services.drive.model.File;

import java.util.ArrayList;
import java.util.Date;

public class FirstFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private ListView cloudDiskView;

    private ItemAdapter itemAdapter;

    private ArrayList<File> fileArrayList = new ArrayList<>();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        cloudDiskView = view.findViewById(R.id.cloudDiskView);

        // test code
        fileArrayList.add(getExampleFile());

        if (itemAdapter == null) {
            itemAdapter = new ItemAdapter(fileArrayList, getContext());
        } else {
            itemAdapter.setFileList(fileArrayList);
        }
        cloudDiskView.setAdapter(itemAdapter);
        cloudDiskView.setOnItemClickListener(this);
        cloudDiskView.setOnItemLongClickListener(this);

        Button refreshButton = view.findViewById(R.id.bt_refresh);
        TextView textView = view.findViewById(R.id.et_text);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DriveLogic.getInstance().queryFiles(textView.getText().toString(), fileArrayList);
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.drive.action.refreshList");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                if ("com.example.drive.action.refreshList".equals(intent.getAction())) {
                    itemAdapter.setFileList(fileArrayList);
                    itemAdapter.notifyDataSetChanged();
                }
            }
        }, intentFilter);
        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private File getExampleFile() {
        return new File()
                .setId("my_test_id")
                .setFileName("test.jpg")
                .setCategory("drive#file")
                .setMimeType("image/jpeg")
                .setCreatedTime(new DateTime(new Date()));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO: some action code
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if ("application/vnd.huawei-apps.folder".equals(fileArrayList.get(position).getMimeType())) {
            return true;
        }
        PopupMenu popupMenu = new PopupMenu(this.getActivity(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener((item) -> {
            if (item.getItemId() == R.id.action_download) {
                // TODO download to local storage
            }
            return true;
        });
        popupMenu.show();
        return true;
    }
}