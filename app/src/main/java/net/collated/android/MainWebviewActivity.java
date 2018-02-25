package net.collated.android;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainWebviewActivity extends AppCompatActivity implements HttpHelper.VolleyCallback {

    private static final String TAG = MainWebviewActivity.class.getSimpleName();

    private WebView webview;

    private String token;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    ProgressDialog progressDialog;

    HttpHelper httpHelper;
    private Toolbar toolbar;

    String shareUrlWhenUserNotLoggedIn;
    String shareTitleWhenUserNotLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_webview);

        toolbar = (Toolbar)findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri uri = getIntent().getData();

            Log.i(TAG, "onCreate uri: " + uri.toString());

            if (uri != null && uri.toString().length() > 0) {

                token = parseToken(uri);

            }
        }

        httpHelper = new HttpHelper(this);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            WebView.setWebContentsDebuggingEnabled(true); // allows remote webview debugging via Chrome
//        }

        setupUI();

        token = prefs.getString(Const.PREFS_TOKEN, null);

        loadDefaultPage();

        if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && !progressDialog.isShowing()) progressDialog.show();

    }

    private String parseToken(Uri uri) {
        token = uri.toString().replaceFirst("net.collated.android://", "");
        token = token.replaceFirst("#_=_", "");
        editor.putString(Const.PREFS_TOKEN, token).commit();
        invalidateOptionsMenu();

        return token;
    }

    private void loadDefaultPage() {

        Log.i(TAG, "token: " + token);

        if (token == null) {
            loadSignInPage();
        } else {
            findViewById(R.id.navigation_icon).setVisibility(View.VISIBLE);
            invalidateOptionsMenu();
            webview.loadUrl(Const.SITE_URL + Const.URL_PARAMS + token);
        }
    }

    private void loadSignInPage() {
        webview.loadUrl(Const.LOCAL_SIGN_IN_PAGE);
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent action: " + intent.getAction());

        Bundle extras = intent.getExtras();

        if (extras != null) {
            shareTitleWhenUserNotLoggedIn = extras.getString(Const.EXTRAS_SHARE_TITLE);
            shareUrlWhenUserNotLoggedIn = extras.getString(Const.EXTRAS_SHARE_URL);

            Log.i(TAG, "shared: " + shareTitleWhenUserNotLoggedIn + " / " + shareUrlWhenUserNotLoggedIn);
        } else {
            Log.i(TAG, "shared: no extras" );

        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();

            token = parseToken(uri);

            loadDefaultPage();
        }
    }



    void handleInboundShare(String sharedUrl, String sharedPageTitle) {

        if (sharedUrl != null) {

            Log.i(TAG, "handleInboundShare: " + sharedUrl + ", " + sharedPageTitle);

            if (token == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            if (token.contains("#_=_")) {
                token = token.replaceFirst("#_=_","");
            }
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put(Const.TOKEN, token);
                jsonBody.put(Const.URL_ARR, sharedUrl);
                jsonBody.put(Const.TITLE_ARR, sharedPageTitle);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            httpHelper.postShareInfo(jsonBody, this);
        }
    }

    private void setupUI() {

        progressDialog = new ProgressDialog(this, android.R.style.Theme_DeviceDefault_Light_Dialog);
        progressDialog.setMessage("Loading...");

        toolbar = (Toolbar)findViewById(R.id.my_toolbar);


        webview = (WebView) findViewById(R.id.webview);



        webview.isDrawingCacheEnabled();
        webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setAllowContentAccess(true);
//        webview.getSettings().setSupportMultipleWindows(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.setWebViewClient(new WebViewClient() {


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "onpage shouldOverrideUrlLoading: " + url);

//                if (url.contains("https")) url = url.replaceFirst("https", "http");

//                if (url.contains("#_=_")) url = url.replaceFirst("#_=_", "");

                // if the URL matches the custom URL schema which returns the token from the Facebook/Twitter/LinkedIn login flow...
                if (url.startsWith("net.collated.android://")) {

                    // Parse the token fom the URL
                    token = url.replaceFirst("net.collated.android://", "");
                    Log.i(TAG, "token: " + token);

                    // save to user prefs
                    editor.putString(Const.PREFS_TOKEN, token).commit();

//                    if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

                    loadDefaultPage();
//                    if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && !progressDialog.isShowing()) progressDialog.show();

                    return true;

                    // otherwise proceed as normal
                } else {
                    Log.i(TAG, "url: " + url);
//                    if (url.contains("//app.collated.net") || url.contains("//www.collated.net")) {
                    if (    url.contains("collated.net") ||
                            url.contains("api.twitter.com/oauth/authenticate") ||
                            url.contains("api.twitter.com/login/error") ||
                            url.contains("slack.com/oauth")
                            ) {
                        if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && !progressDialog.isShowing()) progressDialog.show();

                        if (shareUrlWhenUserNotLoggedIn != null) {
                            handleInboundShare(shareUrlWhenUserNotLoggedIn, shareTitleWhenUserNotLoggedIn);
                            shareUrlWhenUserNotLoggedIn = null;
                        }

                        return super.shouldOverrideUrlLoading(view, url);
                    } else {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                        return true;
                    }
                }
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onpagefinished: " + url);

//                if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && !progressDialog.isShowing()) progressDialog.show();
                // If you are trying to update the collated_user_script.js, it won't work
                // unless you minify it and replace all double quote with singles. See
                // the assets folder for more info including the original JS file
                try {
                    webview.evaluateJavascript(Utils.readFromAssets(MainWebviewActivity.this, "collated_user_script.js"), new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String s) {

                                    Log.i(TAG, "onReceiveValue: "  + s);
                                    if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

                                }
                            }
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        if (!MainWebviewActivity.this.isFinishing() && progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        Log.i(TAG, "onCreateOptionsMenu - token ==" + token);
        if (token != null) {
            inflater.inflate(R.menu.main_menu, menu);
        }
        return true;
    }


    /**
     * Add support for back button to navigate the webview history if possible
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webview.canGoBack()) {
                        webview.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    public void onButtonClicked(View v) {

        switch (v.getId()) {
            case R.id.navigation_icon :

                Log.i(TAG, "navicon click");
                webview.evaluateJavascript("javascript:CollatedUserScript.toggleSidebar()", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        Log.i(TAG, "onReceiveValue");
                    }
                });
                break;

        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.i(TAG, "item: " + item.getItemId());

        switch (item.getItemId())
        {
            case android.R.id.home:
                Log.i(TAG, "home button clicked");

                return true;
            case R.id.sign_out:

                AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
                builder.setTitle("Sign Out");
                builder.setMessage("Are you sure you want to sign out?");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        // remove token from user prefs
                        editor.putString(Const.PREFS_TOKEN, null).commit();
                        token = null;

                        // clear cache and history
                        webview.clearCache(true);
                        webview.clearHistory();

                        // delete any cookies we have stored
//                        Utils.clearCookies(MainWebviewActivity.this);

                        invalidateOptionsMenu();

                        findViewById(R.id.navigation_icon).setVisibility(View.INVISIBLE);

                        loadSignInPage();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        // no action required
                    }
                });
                builder.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onVolleyResult(int calltype, String result, VolleyError error) {

        if (error == null) loadDefaultPage();

    }

}
