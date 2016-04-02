package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.HistColumns;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;

/**
 * Created by ahmed on 4/1/16.
 */
public class FetchPlotDataService  extends IntentService{
    public final static String BROADCAST_ACTION = "com.sam_chordas.android.stockhawk.app.BROADCAT_ACTION";
    private String LOG_TAG = FetchPlotDataService.class.getSimpleName();
    static  HttpUrl url = HttpUrl.parse("http://ichart.finance.yahoo.com/table.csv");
    private final OkHttpClient client = new OkHttpClient();
    private Context mContext =  this;



    public FetchPlotDataService(){
        super(FetchPlotDataService.class.getName());
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FetchPlotDataService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri uri = intent.getData();
        fetchPoints(uri);
//        sendBroadcast(new Intent(this, StockDetailActivity.class));
    }

    private void fetchPoints(final Uri uri) {
        final String symbol = uri.getLastPathSegment();

//        if()
            DateTime dt = new DateTime(new Date());

            HttpUrl myUrl = url.newBuilder()
//        s 	Ticker symbol (YHOO in the example)
//        a     The "from month"
//        b 	The "from day"
//        c 	The "from year"
//        d 	The "to month"
//        e 	The "to day"
//        f 	The "to year"
//        g 	d for day, m for month, y for yearly
                    .addQueryParameter("s", symbol)
                    .addQueryParameter("a", String.valueOf(dt.getMonthOfYear()))
                    .addQueryParameter("b", String.valueOf(dt.getDayOfMonth()))
                    .addQueryParameter("c", String.valueOf(dt.getYear() - 1))
//                .addQueryParameter("d", String.valueOf(dt.getMonthOfYear()))
//                .addQueryParameter("e", String.valueOf(dt.getDayOfMonth()))
//                .addQueryParameter("f",String.valueOf(dt.getYear()))
                    .addQueryParameter("g", "m")
                    .addQueryParameter("ignore", ".csv")
                    .build();
            Log.d(LOG_TAG, myUrl.toString());

            Request request = new Request.Builder().url(myUrl).build();
        client.newCall(request).enqueue(new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
            }

            @Override
            public void onResponse(Response response) throws IOException {
                ContentValues contentValues;
                BufferedReader reader = new BufferedReader(
                        response.body().charStream()
                );
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);
                for (CSVRecord record : records) {
//                    ContentResolver.
                    Log.d(LOG_TAG, (record.get("Date")));
                    contentValues = new ContentValues();
                    contentValues.put(HistColumns.SYMBOL, symbol);
                    contentValues.put(HistColumns.DATE, record.get("Date"));
                    contentValues.put(HistColumns.VALUE, record.get("Close"));
//                        TODO bulk insert
                    mContext.getContentResolver().insert(
                            uri, contentValues);
                }
            }
        });
        Intent done = new Intent(BROADCAST_ACTION);
        done.putExtra("symbol",uri.getLastPathSegment());
        LocalBroadcastManager.getInstance(this).sendBroadcast(done);
        }
}
