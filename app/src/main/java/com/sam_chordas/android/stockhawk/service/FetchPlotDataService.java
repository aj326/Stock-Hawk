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
//        sendBroadcast(new Intent(this, ChartActivity.class));
    }

    private void fetchPoints(final Uri uri) {
        final String symbol = uri.getLastPathSegment();

//        if()
            DateTime dt = new DateTime(new Date());

            HttpUrl myUrl = url.newBuilder()
//       after countless hours of failing to parse yql, I resorted to this hidden yahoo REST API :(

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
                    .addQueryParameter("b", "1")
                    .addQueryParameter("c", String.valueOf(dt.getYear() - 1))
                    .addQueryParameter("d", String.valueOf(dt.getMonthOfYear()-1))
//                .addQueryParameter("e", String.valueOf(dt.getDayOfMonth()))
//                .addQueryParameter("f",String.valueOf(dt.getYear()))
                    .addQueryParameter("g", "m")
                    .addQueryParameter("ignore", ".csv")
                    .build();
            Log.d(LOG_TAG, "URL: " +myUrl.toString());

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
                String[] DATES = {HistColumns.DATE_0,HistColumns.DATE_1,HistColumns.DATE_2
                ,HistColumns.DATE_3,HistColumns.DATE_4,HistColumns.DATE_5,HistColumns.DATE_6,
                                  HistColumns.DATE_7,HistColumns.DATE_8,HistColumns.DATE_9,
                                  HistColumns.DATE_10,HistColumns.DATE_11};
                String[] VALUES = {HistColumns.VALUE_0,HistColumns.VALUE_1,HistColumns.VALUE_2
                        ,HistColumns.VALUE_3,HistColumns.VALUE_4,HistColumns.VALUE_5,HistColumns.VALUE_6,
                                  HistColumns.VALUE_7,HistColumns.VALUE_8,HistColumns.VALUE_9,
                                  HistColumns.VALUE_10,HistColumns.VALUE_11};
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(reader);
                int i = (DATES.length)-1;
                contentValues = new ContentValues();
                for(CSVRecord record: records){
                    Log.d(LOG_TAG, (record.get("Date")));
                    contentValues.put(HistColumns.SYMBOL, symbol);
                    contentValues.put(DATES[i], record.get("Date"));
                    contentValues.put(VALUES[i], record.get("Close"));
                    i--;
                }
                mContext.getContentResolver().insert(
                        uri, contentValues);
            }
        });
        Intent done = new Intent(BROADCAST_ACTION);
        done.putExtra("symbol",uri.getLastPathSegment());
        LocalBroadcastManager.getInstance(this).sendBroadcast(done);
        }
}
