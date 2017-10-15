package mikio.gsonnewsapi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
//TODO: make Imagesize in ImaveView consistent
//TODO: exchange SeekBar for Next/Prev Button
//TODO: create more Display space / militarize controls as much as possible
//TODO: or present the top 10 Headlines in a list (for each source)
//TODO: fix error, if source got no content (e.g. washington post [9])

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AdapterView.OnItemSelectedListener {
    private String _MyJSONStream;
    private String _Headline;
    private String _Text;
    private String _ImageURL;
    private String _Date;
    private int _ArticleNr;

    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Checking Networkconnetction
        if (!isNetworkAvailable()) {
            showMessage("Network unavailable.");
        }
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);

        Spinner spinner = (Spinner) findViewById(R.id.spinnerDropDownSelection);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_items, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateData();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int n = seekBar.getProgress() + 1;
        Toast.makeText(this, "Read Headline " + n, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateData();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private int getSeekBarProgess(SeekBar seekBar) {
        return seekBar.getProgress();
    }

    private void updateData() {
        final String sourceName = convertToAPISourceName(getSourceName());
        final String sortType = getAppropriateSortType(sourceName);
        _ArticleNr = getSeekBarProgess(seekBar);
        Runnable runConnection = new Runnable() {
            @Override
            public void run() {
                //Do this in Thread:
                StringBuilder stringBuilder = new StringBuilder();
                String urlString = "https://newsapi.org/v1/articles?source=" + sourceName + "&sortBy=" + sortType + "&apiKey=add1069ed16746728299fddacb017619";
                try {
                    URL urlEndpoint = new URL(urlString);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) urlEndpoint.openConnection();

                    if (isResponseCodeOK(httpURLConnection)) {
                        InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            showMessage("IOException was thrown.");
                        }
                        handleJSONContent(stringBuilder, httpURLConnection);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateDisplay();
                            }
                        });
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread establishConnection = new Thread(runConnection);
        establishConnection.start();
    }

    private void handleJSONContent(StringBuilder stringBuilder, HttpURLConnection httpURLConnection) throws JSONException {
        _MyJSONStream = stringBuilder.toString();
        httpURLConnection.disconnect();

        News newsObject = deserializeJson(_MyJSONStream);
        List<Article> articles = newsObject.getArticles();
        Article article = articles.get(_ArticleNr);

        _Headline = article.getTitle();
        _ImageURL = article.getUrlToImage();
        _Date = article.getPublishedAt();
        _Text = article.getDescription();
    }

    private void loadImage() {
        ImageView imageView = (ImageView) findViewById(R.id.ivImage);
        //load from online source:
        if (!_ImageURL.isEmpty()) Picasso.with(MainActivity.this).load(_ImageURL).into(imageView);
    }

    /**
     * @param jsonStream receives a JsonStream String
     * @return a News Object
     */
    private News deserializeJson(String jsonStream) {
        //Deserialize with Gson --- TDOD: Put this in Method!
        Gson gson = new Gson();
        return gson.fromJson(jsonStream, News.class);
    }

    private boolean isResponseCodeOK(HttpURLConnection httpURLConnection) throws IOException {
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) return true;
        else return false;
    }

    private void showMessage(String string) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }

    private void updateDisplay() {
        TextView tvH1 = (TextView) findViewById(R.id.tvH1);
        TextView tvText1 = (TextView) findViewById(R.id.tvText1);
        TextView tvDate1 = (TextView) findViewById(R.id.tvDate1);

        tvH1.setText(_Headline);
        loadImage();
        tvText1.setText(_Text);
        tvDate1.setText(_Date);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager mgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mgr.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private String getSourceName() {
        Spinner spinnerDropDownSelection = (Spinner) findViewById(R.id.spinnerDropDownSelection);
        if (spinnerDropDownSelection.getSelectedItem().toString().length() == 0) {
            return "die-zeit";
        } else return spinnerDropDownSelection.getSelectedItem().toString();
    }

    private String convertToAPISourceName(String sourceName) {
        String APISourceName = "";
        switch (sourceName) {
            case "Zeit Online":
                APISourceName = "die-zeit";
                break;
            case "Spiegel Online":
                APISourceName = "spiegel-online";
                break;
            case "BBC News":
                APISourceName = "bbc-news";
                break;
            case "BBS Sport":
                APISourceName = "bbc-sport";
                break;
            case "CNBC":
                APISourceName = "cnbc";
                break;
            case "CNN":
                APISourceName = "cnn";
                break;
            case "Focus":
                APISourceName = "focus";
                break;
            case "Handelsblatt":
                APISourceName = "handelsblatt";
                break;
            case "Tagesspiegel":
                APISourceName = "der-tagesspiegel";
                break;
            case "Google News":
                APISourceName = "google-news";
                break;
            case "National Geographic":
                APISourceName = "national-geographic";
                break;
            case "Independent":
                APISourceName = "independent";
                break;
            case "Guardian UK":
                APISourceName = "the-guardian-uk";
                break;
            case "Huffington Post":
                APISourceName = "the-huffington-post";
                break;
            case "Washington Post":
                APISourceName = "the-washington-post";
                break;
        }
        return APISourceName;
    }

    private String getAppropriateSortType(String sourceName) {
        if (sourceName.equals("die-zeit") || sourceName.equals("der-tagesspiegel") || sourceName.equals("handelsblatt")) {
            return "latest";
        } else return "top";
    }


}