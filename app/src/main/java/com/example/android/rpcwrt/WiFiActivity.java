package com.example.android.rpcwrt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.example.android.rpcwrt.adapter.MyAdapter;
import com.example.android.rpcwrt.model.Item;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionOptions;

import net.minidev.json.*;


import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WiFiActivity extends BaseActivity {

    private WiFiTask wiFiTask;
    private RecyclerView recyclerView;
    private MyAdapter myAdapter;
    private List<JSONObject> iwInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        initializeToolbar();
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

    private class WiFiTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            showProgressBar();
            hideFailButtons();
            try {
                session = new JSONRPC2Session(new URL(baseUrl + LUCI_RPC_PATH + "sys?auth=" + token));
                JSONRPC2SessionOptions options = session.getOptions();
                options.setConnectTimeout(5000);
                options.setReadTimeout(10000);
                options.trustAllCerts(true);
            } catch (MalformedURLException e) {
                doLogout();
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                int requestId = 0;
                // first get available radio devices
                List<Object> params = new ArrayList<>();
                params.add("ubus call iwinfo devices");
                request = new JSONRPC2Request("exec", params, requestId);;
                response = session.send(request);
                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getResult().toString());
                JSONArray wifidevs = (JSONArray) jsonObject.get("devices");

                iwInfoList = new ArrayList<>();
                for (Object wifidev: wifidevs) {
                    params = new ArrayList<>();
                    params.add(wifidev);
                    request = new JSONRPC2Request("wifi.getiwinfo", params, requestId);
                    response = session.send(request);
                    iwInfoList.add((JSONObject) JSONValue.parse(response.getResult().toString()));
                }
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
                for (JSONObject iwInfo: iwInfoList) {
                    myAdapter.addItem(new Item((String)iwInfo.get("ifname"), ""));
                    myAdapter.addItem(new Item("SSID",(String)iwInfo.get("ssid")));
                    JSONObject encryption = (JSONObject)iwInfo.get("encryption");
                    myAdapter.addItem(new Item("Encryption", (String)encryption.get("description")));
                    DecimalFormat df = new DecimalFormat("#.#");
                    df.setRoundingMode(RoundingMode.FLOOR);
                    myAdapter.addItem(new Item("Frequency", df.format((Integer)iwInfo.get("frequency")/1000.0)+ "GHz"));
                    myAdapter.addItem(new Item("Channel", ((Integer)iwInfo.get("channel")).toString()));
                    myAdapter.addItem(new Item("Hardware", (String)iwInfo.get("hardware_name")));
                    myAdapter.addItem(new Item("BSSID (MAC Address)", (String)iwInfo.get("bssid")));
                    myAdapter.addItem(new Item("Transmit Power", ((Integer)iwInfo.get("txpower")).toString() + "dBm"));
                    Object assoc = iwInfo.get("assoclist");
                    if (assoc instanceof JSONObject) {
                        try {
                            JSONObject assocList = (JSONObject) assoc;
                            StringBuilder stas = new StringBuilder();
                            for (Iterator it = assocList.entrySet().iterator(); it.hasNext(); ) {
                                Map.Entry entry = (Map.Entry) it.next();
                                stas.append("MAC: ").append(entry.getKey()).append('\n');
                                String vendor = queryOUI((String) entry.getKey());
                                if (vendor != null)
                                    stas.append("Vendor: ").append(vendor).append('\n');
                                stas.append('\n');
                            }
                            myAdapter.addItem(new Item("Associated Stations", stas.toString()));
                            // System.out.print(stas.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        myAdapter.addItem(new Item("Associated Stations", "None"));
                    }
                }
            } else {
                printFailMessage();
                showFailButtons();
                // doLogout();
            }
            hideProgressBar();
        }
    }

    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (wiFiTask != null && !wiFiTask.isCancelled()) {
            wiFiTask.cancel(true);
            wiFiTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( wiFiTask == null || wiFiTask.getStatus() != AsyncTask.Status.RUNNING) {
            wiFiTask = new WiFiTask();
            wiFiTask.execute();
        }
    }
}
