package com.example.android.rpcwrt;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionOptions;

import java.net.MalformedURLException;
import java.net.URL;

public class RebootActivity extends BaseActivity {
    private Button rebootButton;
    private RebootTask rebootTask;
    private TextView rebootText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reboot);

        initializeToolbar();
        rebootText = findViewById(R.id.reboot_text);
        progressBar = findViewById(R.id.progress_bar);
        rebootButton = findViewById(R.id.reboot_btn);
        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
    }

    private class RebootTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            try {
                showProgressBar();
                rebootText.setVisibility(View.INVISIBLE);
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
                request = new JSONRPC2Request("reboot", requestId);
                response = session.send(request);
            } catch (JSONRPC2SessionException e) {
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
                Toast.makeText(RebootActivity.this, "Reboot successful, please wait ~1min to reconnect", Toast.LENGTH_LONG).show();
                doLogout();
            } else {
                hideProgressBar();
                rebootText.setVisibility(View.VISIBLE);
                printFailMessage();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (rebootTask != null && !rebootTask.isCancelled()) {
            rebootTask.cancel(true);
            rebootTask = null;
        }
        super.onDestroy();
    }

    private void connect() {
        // Avoid creation of multiple requests
        if ( rebootTask == null || rebootTask.getStatus() != AsyncTask.Status.RUNNING) {
            rebootTask = new RebootTask();
            rebootTask.execute();
        }
    }
}
