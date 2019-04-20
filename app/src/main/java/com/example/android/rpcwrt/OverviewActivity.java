/*
 * Copyright (C) 2017 Tnno Wu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.rpcwrt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.rpcwrt.adapter.MyAdapter;
import com.example.android.rpcwrt.model.Item;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class OverviewActivity extends BaseActivity {

    private TextView wifiText;
    private OverviewTask overviewTask;
    private RecyclerView recyclerView;
    private MyAdapter myAdapter;

    private Integer uptime;
    private Integer memTotal;
    private Integer memAvail;
    private String hostname;
    private String model;
    private String kernelVersion;
    private String osVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        initializeToolbar();
        // wifiText = findViewById(R.id.wifi_text);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(myAdapter = new MyAdapter());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        progressBar = findViewById(R.id.progress_bar);
        retryButton = findViewById(R.id.retry_btn);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        logOutButton = findViewById(R.id.logout_btn);
        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogout();
            }
        });

        connect();
    }

    private class OverviewTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            try {
                session = new JSONRPC2Session(new URL(baseUrl + LUCI_RPC_PATH + "sys?auth=" + token));
                JSONRPC2SessionOptions options = session.getOptions();
                options.setConnectTimeout(5000);
                options.setReadTimeout(10000);
                options.trustAllCerts(true);
            } catch (MalformedURLException e) {
                // baseUrl is empty. Let's go back to login page
                doLogout();
            }  finally {
                // String method = "wifi.getiwinfo";
                // int requestId = 0;
                // List<Object> params = new ArrayList<>();
                // params.add("wlan0");
                // request = new JSONRPC2Request(method, params, requestId);
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                int requestId = 0;
                // get uptime
                List<Object> params = new ArrayList<>();
                params.add("ubus call system info");
                request = new JSONRPC2Request("exec", params, requestId);
                response = session.send(request);
                JSONObject jsonObject = (JSONObject)JSONValue.parse(response.getResult().toString());
                uptime = (Integer) jsonObject.get("uptime");
                JSONObject childJsonObject = (JSONObject)jsonObject.get("memory");
                memTotal = (Integer) childJsonObject.get("total");
                memAvail = (Integer) childJsonObject.get("free");
                memAvail += (Integer) childJsonObject.get("buffered");
                /* use ubus call to get board info
                   Response is like
                {
                        "kernel": "4.14.111",
                        "hostname": "TP-Link",
                        "system": "MediaTek MT7620A ver:2 eco:6",
                        "model": "TP-Link Archer C5 v4",
                        "board_name": "tplink,c5-v4",
                        "release": {
                            "distribution": "OpenWrt",
                            "version": "SNAPSHOT",
                            "revision": "r9867-43e8c37",
                            "target": "ramips/mt7620",
                            "description": "OpenWrt SNAPSHOT r9867-43e8c37"
                        }
                 } */
                params = new ArrayList<>();
                params.add("ubus call system board");
                request = new JSONRPC2Request("exec", params, requestId);
                response = session.send(request);
                jsonObject = (JSONObject)JSONValue.parse(response.getResult().toString());
                hostname = (String)jsonObject.get("hostname");
                kernelVersion = (String) jsonObject.get("kernel");
                model = (String) jsonObject.get("model");
                childJsonObject = (JSONObject)jsonObject.get("release");
                osVersion = (String) childJsonObject.get("description");

            } catch (JSONRPC2SessionException e) {
                // clear previous response
                e.printStackTrace();
                response = null;
            } catch (Exception e) {
                e.printStackTrace();
                response = null;
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if  (response != null && response.indicatesSuccess() && response.getResult() != null) {
                parseJSONStringAndUpdateUI(response.getResult().toString());
            } else {
                printFailMessage();
                showFailButtons();
                hideProgressBar();
                // doLogout();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (overviewTask != null && !overviewTask.isCancelled()) {
            overviewTask.cancel(true);
            overviewTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( overviewTask == null || overviewTask.getStatus() != AsyncTask.Status.RUNNING) {
            overviewTask = new OverviewTask();
            overviewTask.execute();
        }
    }

    private void parseJSONStringAndUpdateUI(String jsonString) {
        hideFailButtons();
        hideProgressBar();
        myAdapter.addItem(new Item("URL", baseUrl));
        myAdapter.addItem(new Item("Model", model));
        myAdapter.addItem(new Item("OS Version", osVersion));
        myAdapter.addItem(new Item("Kernel Version", kernelVersion));
        myAdapter.addItem(new Item("Hostname", hostname));
        myAdapter.addItem(new Item("Uptime", formatUptime(uptime)));
        myAdapter.addItem(new Item("RAM (Free/Total)", String.valueOf(memAvail/1024/1024) + "/" + String.valueOf(memTotal/1024/1024) + " MiB" ));
        // JSONArray jsonArray = (JSONArray) JSONValue.parse(jsonString);

    }

    private String formatUptime(Integer uptime) {
        return String.format("%dd %02dh %02dm %02ds", uptime / (3600 * 24), (uptime % (3600 * 24)) / 3600, (uptime % 3600) / 60, (uptime % 60));
    }
}
