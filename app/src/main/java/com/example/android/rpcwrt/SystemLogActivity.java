package com.example.android.rpcwrt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SystemLogActivity extends BaseActivity {

    private static final String TAG = "SystemLogActivity";
    private LogTask logTask;
    private TextView log;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_systemlog);

        initializeToolbar();
        log = findViewById(R.id.log_text);
        // log.setMovementMethod(new ScrollingMovementMethod());
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
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ClearLogTask().execute();
            }
        });

        ScrollView scrollView = findViewById(R.id.scroll_view);
        scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY - oldScrollY > 0)
                    fab.hide();
                else
                    fab.show();
            }
        });

        connect();
    }

    private class LogTask extends AsyncTask<String, Integer, String> {
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
                // baseUrl is empty. Let's go back to login page
                doLogout();
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                int requestId = 0;
                request = new JSONRPC2Request("syslog", requestId);
                response = session.send(request);
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
                log.setText(response.getResult().toString());

            } else {
                printFailMessage();
                showFailButtons();
            }
            hideProgressBar();
        }
    }

    private class ClearLogTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            log.setText("");
            showProgressBar();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                int requestId = 0;
                List<Object> params = new ArrayList<>();
                params.add("/etc/init.d/log restart");
                request = new JSONRPC2Request("call", params, requestId);
                response = session.send(request);
            } catch (Exception e) {}
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            connect();
            Toast.makeText(SystemLogActivity.this, "Log cleared", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (logTask != null && !logTask.isCancelled()) {
            logTask.cancel(true);
            logTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( logTask == null || logTask.getStatus() != AsyncTask.Status.RUNNING) {
            logTask = new LogTask();
            logTask.execute();
        }
    }
}
