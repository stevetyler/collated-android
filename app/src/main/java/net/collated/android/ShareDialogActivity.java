package net.collated.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;

public class ShareDialogActivity extends AppCompatActivity implements HttpHelper.VolleyCallback {

    private static final String TAG = ShareDialogActivity.class.getSimpleName();
    private String token;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    HttpHelper httpHelper;

    EditText shareTitleEdit;
    ImageView shareImage;
    private String imageUrl;
    private Intent intent;
    private JSONObject jsonBody;
    private String sharedUrl;
    private String sharedPageTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_dialog);
        setTitle("Share to Collated");

        this.setFinishOnTouchOutside(false);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();

        httpHelper = new HttpHelper(this);

        shareTitleEdit = (EditText)findViewById(R.id.share_title_edit);

        shareImage = (ImageView)findViewById(R.id.share_image);
    }


    @Override
    protected void onResume() {
        super.onResume();

        token = prefs.getString(Const.PREFS_TOKEN, null);

        intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (intent.getType() != null && intent.getType().equals("text/plain")) {
                handleInboundShare(intent); // Handle text being sent
            }
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String code = uri.getQueryParameter("code");
            Log.i(TAG, "code: " + code);
        }
    }


    private class DownloadFilesTask extends AsyncTask<String, Integer, Long> {

        protected Long doInBackground(String... urls) {
            parseThumbnail(urls[0]);
            return 0l;
        }


        protected void onPostExecute(Long result) {
            Log.i(TAG, "onPostExecute");

            if (this != null && !isFinishing()) Glide.with(ShareDialogActivity.this).load(imageUrl).into(shareImage);

        }
    }


    private void parseThumbnail(String url) {
        Document doc= null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (doc != null) {
            Elements elements=doc.select("meta");

            for(Element e: elements){
                //fetch image url from content attribute of meta tag.
                imageUrl = e.attr("content");

                //OR more specifically you can check meta property.
                if(e.attr("property").equalsIgnoreCase("og:image")){
                    imageUrl = e.attr("content");

                    Log.i(TAG, "imageUrl: " + imageUrl);
                    break;
                }
            }

        }
    }

    void handleInboundShare(Intent intent) {
        sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedUrl != null) {

            new DownloadFilesTask().execute(sharedUrl);

            sharedPageTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT);

            shareTitleEdit.setText(sharedPageTitle);

            String sharedPageStream = intent.getStringExtra(Intent.EXTRA_STREAM);

            Log.i(TAG, "handleInboundShare: " + sharedUrl + ", " + sharedPageTitle + ", " + sharedPageStream + " [" + intent.getExtras().describeContents() + "]");

        }
    }

    @Override
    public void onVolleyResult(int calltype, String result, VolleyError error) {
        Toast.makeText(this, "Shared link to Collated", Toast.LENGTH_SHORT).show();
        finish();
    }



    public void onButtonClicked(View v) {

        switch (v.getId()) {
            case R.id.share_cancel_button :

                finish();
                break;

            case R.id.share_save_button :

                if (token == null) {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, MainWebviewActivity.class);
                    i.putExtra(Const.EXTRAS_SHARE_TITLE, shareTitleEdit.getText().toString());
                    i.putExtra(Const.EXTRAS_SHARE_URL, sharedUrl);

                    Log.i(TAG, "token == null : " + sharedUrl + ", " + sharedPageTitle);

                    startActivity(i);
                    return;
                }

                if (token.contains("#_=_")) {
                    token = token.replaceFirst("#_=_","");
                }
                jsonBody = new JSONObject();
                try {
                    jsonBody.put(Const.TOKEN, token);
                    jsonBody.put(Const.URL_ARR, sharedUrl);
                    jsonBody.put(Const.TITLE_ARR, shareTitleEdit.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (jsonBody != null) httpHelper.postShareInfo(jsonBody, this);

                break;
        }
    }

}
