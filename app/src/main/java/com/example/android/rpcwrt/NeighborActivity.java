package com.example.android.rpcwrt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.example.android.rpcwrt.adapter.MyAdapter;
import com.example.android.rpcwrt.model.Item;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionOptions;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class NeighborActivity extends BaseActivity {

    private NeighborTask neighborTask;
    private RecyclerView recyclerView;
    private MyAdapter myAdapter;
    private JSONObject hostHints;

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

    private class NeighborTask extends AsyncTask<String, Integer, String> {
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
                request = new JSONRPC2Request("net.host_hints", requestId);
                response = session.send(request);
                hostHints = (JSONObject) JSONValue.parse(response.getResult().toString());
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
            if  (response != null && response.indicatesSuccess() && response.getResult() != null && hostHints != null) {
                    List arrayList = new ArrayList<>(hostHints.keySet());
                    Collections.sort(arrayList, new Comparator() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            return o1.toString().compareTo(o2.toString());
                        }
                    });

                    for (Iterator it = arrayList.iterator(); it.hasNext();) {
                        String key = (String) it.next();
                        StringBuilder builder = new StringBuilder();
                        JSONObject info = (JSONObject) hostHints.get(key);

                        // Ignore hosts without IPv4 address
                        String ipv4 = (String) info.get("ipv4");
                        if (ipv4 == null) continue;
                        String ipv6 = (String) info.get("ipv6");
                        String name = (String) info.get("name");
                        String vendor = queryOUI(key);
                        if (vendor != null)
                            builder.append("Vendor: ").append(vendor).append('\n');
                        if (name != null)
                            builder.append("Hostname: ").append(name).append('\n');
                        builder.append("IPv4: ").append(ipv4);
                        if (ipv6 != null)
                            builder.append('\n').append("IPv6: ").append(ipv6);
                        myAdapter.addItem(new Item(key, builder.toString()));
                    }
            } else {
                printFailMessage();
                showFailButtons();
            }
            hideProgressBar();
        }
    }

    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (neighborTask != null && !neighborTask.isCancelled()) {
            neighborTask.cancel(true);
            neighborTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( neighborTask == null || neighborTask.getStatus() != AsyncTask.Status.RUNNING) {
            neighborTask = new NeighborTask();
            neighborTask.execute();
        }
    }
}
