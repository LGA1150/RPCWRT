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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OpkgListActivity extends BaseActivity {

    private OpkgTask opkgTask;
    private RecyclerView recyclerView;
    private MyAdapter myAdapter;
    private JSONObject opkgList;

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

    private class OpkgTask extends AsyncTask<String, Integer, String> {
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
                List<Object> params = new ArrayList<>();
                params.add("ubus call rpc-sys packagelist");
                request = new JSONRPC2Request("exec", params, requestId);
                response = session.send(request);
                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.getResult().toString());
                opkgList = (JSONObject) JSONValue.parse(jsonObject.get("packages").toString());

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
                if (opkgList != null) {
                    List arrayList = new ArrayList<>(opkgList.keySet());
                    Collections.sort(arrayList, new Comparator() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            return o1.toString().compareTo(o2.toString());
                        }
                    });

                    for (Iterator it = arrayList.iterator(); it.hasNext();) {
                        String key = (String) it.next();
                        myAdapter.addItem(new Item(key, opkgList.get(key).toString()));
                    }
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
        if (opkgTask != null && !opkgTask.isCancelled()) {
            opkgTask.cancel(true);
            opkgTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( opkgTask == null || opkgTask.getStatus() != AsyncTask.Status.RUNNING) {
            opkgTask = new OpkgTask();
            opkgTask.execute();
        }
    }
}
