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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;

public class OverviewActivity extends BaseActivity {

    private TextView wifiText;
    private JSONRPC2Session session;
    private JSONRPC2Request request;
    private JSONRPC2Response response;
    private WifiTask wifiTask;
    private ProgressBar progressBar;
    private Button retryButton;
    private RecyclerView recyclerView;
    private MyAdapter myAdapter;
//    private static final String TAG = "OverviewActivity";

    private Long uptime;
    private String hostname;

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

        connect();
    }

    private class WifiTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            try {
                session = new JSONRPC2Session(new URL(baseUrl + LUCI_RPC_PATH + "sys?auth=" + token));
            } catch (MalformedURLException e) {
                // baseUrl is empty. Let's go back to login page
                doLogout();
            }  finally {
                JSONRPC2SessionOptions options = session.getOptions();
                options.setConnectTimeout(5000);
                options.setReadTimeout(10000);
                options.trustAllCerts(true);
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
                request = new JSONRPC2Request("uptime", requestId);
                response = session.send(request);
                uptime = (Long) response.getResult();
                request = new JSONRPC2Request("hostname", requestId);
                response = session.send(request);
                hostname = response.getResult().toString();
            } catch (JSONRPC2SessionException e) {
                // clear previous response
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
                Toast.makeText(OverviewActivity.this, "Invalid response\nSession may be expired",Toast.LENGTH_SHORT).show();
                retryButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                // doLogout();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (wifiTask != null && !wifiTask.isCancelled()) {
            wifiTask.cancel(true);
            wifiTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( wifiTask == null || wifiTask.getStatus() != AsyncTask.Status.RUNNING) {
            wifiTask = new WifiTask();
            wifiTask.execute();
        }
    }

    private void parseJSONStringAndUpdateUI(String jsonString) {
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        // wifiText.setText(jsonString);
        //for (int i = 0; i < 10; i++)
        myAdapter.addItem(new Item("Hostname", hostname));
        myAdapter.addItem(new Item("Uptime", formatUptime(uptime)));
        JSONArray jsonArray = (JSONArray) JSONValue.parse(jsonString);

    }

    private String formatUptime(Long uptime) {
        return String.format("%dd %02dh %02dm %02ds", uptime / (3600 * 24), (uptime % (3600 * 24)) / 3600, (uptime % 3600) / 60, (uptime % 60));
    }
}
