package com.example.bluemix.videodownloadtester;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.db.chart.model.ChartEntry;
import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.RetryPolicy;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();

    public static String SERVER_ADDRESS = "http://192.168.0.100:3000/";
    public static String INFO_ADDR = "download";


    public static String INFO_URL = SERVER_ADDRESS + INFO_ADDR;     // to be retrieved from
    public ThinDownloadManager downloadManager;
    public static final int DOWNLOAD_THREAD_POOL_SIZE = 4;
    public static String VIDEO_DOWNLOAD_URL;

    public static String DOWNLOADED_VIDEO_PATH = "";
    public static final String VIDEO_FILE_NAME = "imagine.mp3";   //  when downloaded, re-named to this, and save it in the sdcard
    MyDownloadStatusListener myDownloadStatusListener = new MyDownloadStatusListener();
    NumberProgressBar number_progress_bar;

    TextView logTxt;
    TextView downloadTxt;
    Button startDownloadButton;


    long downloadTime;
    List<String> downloadTimes = new ArrayList<>();
    Context mContext;

    int repetitions = 1;

    String link_key = "ip_address";
    String default_link = "http://192.168.0.100:3000/file1.mp3";
    LineChartView lineChartView;
    private LineSet lineSet = new LineSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

        number_progress_bar = (NumberProgressBar) findViewById(R.id.number_progress_bar);

        logTxt = (TextView) findViewById(R.id.logTxt);
        downloadTxt = (TextView) findViewById(R.id.downloadTxt);

        startDownloadButton = (Button) findViewById(R.id.startDownloadButton);
        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);

        lineChartView = (LineChartView) findViewById(R.id.linechart);


        SharedPreferences sharedpreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        final SharedPreferences.Editor editor = sharedpreferences.edit();

        final EditText repetitionsEditText = (EditText) findViewById(R.id.repetitionsEditText);

        final EditText serverIPAddress = (EditText) findViewById(R.id.serverIPAddress);

        String lastLink = sharedpreferences.getString(link_key, default_link);
        serverIPAddress.setText(lastLink);

        startDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                downloadManager.cancelAll();
                downloadTimes.clear();

                INFO_URL = serverIPAddress.getText().toString();

                VIDEO_DOWNLOAD_URL = INFO_URL;

                downloadTxt.setText("");

                lineSet.getEntries().clear();
                lineChartView.getData().clear();
                lineChartView.invalidate();

                startDownloadButton.setText("Stop");
                Log.i(TAG, "INFO_URL: " + INFO_URL);

                editor.putString(link_key, VIDEO_DOWNLOAD_URL);
                editor.apply();

                repetitions = Integer.parseInt(repetitionsEditText.getText().toString());

                downloadTheFile();

                Log.i(TAG, "repetitions: " + repetitions);
            }
        });

    }


    private void downloadTheFile() {

        downloadTime = System.currentTimeMillis();

        downloadManager.cancelAll();


        final RetryPolicy retryPolicy = new DefaultRetryPolicy();
        final File filesDir = getExternalFilesDir("");

        Uri downloadUri = Uri.parse(VIDEO_DOWNLOAD_URL);
        final DownloadRequest downloadRequest1 = new DownloadRequest(downloadUri);

        DOWNLOADED_VIDEO_PATH = filesDir + "/" + VIDEO_FILE_NAME;
        Log.i(TAG, "DOWNLOADED_VIDEO_PATH: " + DOWNLOADED_VIDEO_PATH);

        Uri destinationUri = Uri.parse(DOWNLOADED_VIDEO_PATH);

        downloadRequest1.setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.LOW)
                .setRetryPolicy(retryPolicy)
                .setDownloadListener(myDownloadStatusListener);


        downloadManager.add(downloadRequest1);
    }


    class MyDownloadStatusListener implements DownloadStatusListener {
        String logData;

        @Override
        public void onDownloadComplete(int id) {

            Log.i(TAG, "Download " + id + " has completed.");

            downloadTime = System.currentTimeMillis() - downloadTime;

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", new Locale("en"));
            Date date = new Date();
            System.out.println();

            downloadTimes.add(dateFormat.format(date) + ": " + downloadTime + " nano seconds (" + (downloadTime / 1000) + " seconds)");

            //logTxt.setText(logTxt.getText() + "\n" + "downloadTime: " + (downloadTime) + " nano seconds");

            String downloadTxtValue = "";
            for (String timeFragment : downloadTimes) {
                downloadTxtValue += timeFragment +
                        "\n";
            }
            downloadTxt.setText(downloadTxtValue);

            lineSet.addPoint(dateFormat.format(date), downloadTime / 1000);

            if (--repetitions > 0) {
                downloadTheFile();
            } else {
                startDownloadButton.setText("Start");


                if (lineSet.getEntries().size() > 1) {
                    lineSet.setThickness(10f);
                    lineSet.setDotsRadius(4f);
                    lineSet.setSmooth(true);

                    lineSet.setDashed(new float[]{10f, 10f});

                    lineSet.setColor(Color.parseColor("#fccde5"));
                    lineSet.setDotsColor(Color.parseColor("#80b1d3"));
                    lineSet.setDotsStrokeColor(Color.parseColor("#fb8072"));


                    lineChartView.addData(lineSet);

                    lineChartView.setAxisThickness(4);


//              Paint gridPaint = new Paint();
//              gridPaint.setColor(Color.parseColor("#bebada"));
//              gridPaint.setStyle(Paint.Style.STROKE);
//              gridPaint.setAntiAlias(true);
//              gridPaint.setStrokeWidth(Tools.fromDpToPx(.5f));


                    float minValue = lineSet.getEntries().get(0).getValue();
                    float maxValue = lineSet.getEntries().get(0).getValue();
                    for (ChartEntry x : lineSet.getEntries()) {
                        if (minValue > x.getValue()) minValue = x.getValue();
                        if (maxValue < x.getValue()) maxValue = x.getValue();
                    }

                    lineChartView.setAxisColor(Color.parseColor("#80b1d3"))
                            .setAxisBorderValues((int)minValue - 2, (int)maxValue + 2, 2);
//                .setGrid(ChartView.GridType.FULL, gridPaint);


                    lineChartView.addData(lineSet);
                    lineChartView.show();
                }

            }

        }

        @Override
        public void onDownloadFailed(int i, int i1, String s) {

        }


        @Override
        public void onProgress(int id, long totalBytes, long downloadedBytes, int progress) {

            number_progress_bar.setProgress(progress);
            logData = getBytesDownloaded(progress, totalBytes);

            // Log.i(TAG, logData);
            logTxt.setText(logData);
        }
    }

    private String getBytesDownloaded(int progress, long totalBytes) {
        //Greater than 1 MB
        long bytesCompleted = (progress * totalBytes) / 100;

        return bytesCompleted + "/" + totalBytes + " bytes";
//        if (totalBytes >= 1000000) {
//            return ("" + (String.format("%.1f", (float) bytesCompleted / 1000000)) + "/" + (String.format("%.1f", (float) totalBytes / 1000000)) + "MB");
//        }
//        if (totalBytes >= 1000) {
//            return ("" + (String.format("%.1f", (float) bytesCompleted / 1000)) + "/" + (String.format("%.1f", (float) totalBytes / 1000)) + "Kb");
//
//        } else {
//            return ("" + bytesCompleted + "/" + totalBytes);
//        }
    }


    @Override
    protected void onStop() {
        downloadManager.cancelAll();
        super.onStop();
    }

    private String getStr(int id) {
        return mContext.getResources().getString(id);
    }

}
