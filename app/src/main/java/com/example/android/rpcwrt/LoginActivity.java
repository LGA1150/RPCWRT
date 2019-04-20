package com.example.android.rpcwrt;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.util.regex.Pattern;

public class LoginActivity extends BaseActivity {
    private EditText mainUrl;
    private EditText mainPassword;
    private TextView mainText;
    private LoginTask loginTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (isLoggedIn) {
            startActivity(new Intent(LoginActivity.this, OverviewActivity.class));
            finish();
        }

        mainUrl = findViewById(R.id.main_url);
        mainPassword = findViewById(R.id.main_password);
        mainText = findViewById(R.id.main_text);
        progressBar = findViewById(R.id.progress_bar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(loginListener);
        // Logged out from other activity, let's auto-fill the URL
        if (!baseUrl.isEmpty()) {
            mainUrl.setText(baseUrl);
        }
        initializeToolbar();

        new Thread((new Runnable() {
            @Override
            public void run() {
                initializeOUI(LoginActivity.this);
            }
        })).run();
    }

    private final View.OnClickListener loginListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mainUrl.getText().toString().isEmpty())
                Toast.makeText(LoginActivity.this, "Please enter URL", Toast.LENGTH_SHORT).show();
            else {
                try {
                    baseUrl = mainUrl.getText().toString();
                    String pattern = ".*://.*";
                    // Auto adds `http://` protocol
                    if (!Pattern.matches(pattern, baseUrl)) {
                        baseUrl = "http://" + baseUrl;
                        mainUrl.setText(baseUrl);
                    }
                    URL serverURL = new URL(baseUrl + LUCI_RPC_PATH + "auth");
                    session = new JSONRPC2Session(serverURL);
                    JSONRPC2SessionOptions options = session.getOptions();
                    options.setConnectTimeout(5000);
                    options.setReadTimeout(10000);
                    // uhttpd with HTTPS probably uses self-signed certificate
                    options.trustAllCerts(true);
                    String method = "login";
                    int requestId = 0;
                    List<Object> params = new ArrayList<>();
                    // root is only available user
                    params.add("root");
                    params.add(mainPassword.getText().toString());
                    request = new JSONRPC2Request(method, params, requestId);

                    // Avoid creation of multiple requests
                    if (loginTask == null || loginTask.getStatus() != AsyncTask.Status.RUNNING) {
                        loginTask = new LoginTask();
                        loginTask.execute();
                    }
                } catch (MalformedURLException e) {
                    Toast.makeText(LoginActivity.this, "URL \"" + baseUrl + "\" is invalid", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private class LoginTask extends AsyncTask<String, Integer, JSONRPC2Response> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            mainText.setVisibility(View.GONE);
        }

        @Override
        protected JSONRPC2Response doInBackground(String... strings) {
            try {
                response = session.send(request);
            } catch (JSONRPC2SessionException e) {
                // clear previous response
                e.printStackTrace();
                response = null;
            } catch (Exception e) {
                e.printStackTrace();
                response = null;
                return null;
            }
            return response;
        }

        @Override
        protected void onPostExecute(JSONRPC2Response jsonrpc2Response) {
            progressBar.setVisibility(View.GONE);
            mainText.setVisibility(View.VISIBLE);
            if (jsonrpc2Response == null) {
                Toast.makeText(LoginActivity.this, "Invalid response, check the URL and password and try again.", Toast.LENGTH_LONG).show();
            } else if (jsonrpc2Response.indicatesSuccess() && jsonrpc2Response.getResult() != null){
                doLogin();
            } else {
                Toast.makeText(LoginActivity.this, "Login failed, invalid password.", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        // Avoid memory leak
        if (loginTask != null && !loginTask.isCancelled()) {
            loginTask.cancel(true);
            loginTask = null;
        }
        super.onDestroy();
    }

    protected void doLogin() {
        token = response.getResult().toString();
        isLoggedIn = true;
        startActivity(new Intent(LoginActivity.this, OverviewActivity.class));
        finish();
    }
}
