package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * Created by ahmed on 3/12/16.
 */
public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private final String LOG_TAG = StockDetailActivity.class.getSimpleName();

    private LineChartView mChart;
    private final int CURSOR_LOADER_ID = 0;
    private String mSymbol;

    private final Context mContext = this;


    private final String[] mLabels = {
            "1 \'15",
            "2 \'15",
            "3 \'15",
            "4 \'15",
            "5 \'15",
            "6 \'15",
            "7 \'15",
            "8 \'15",
            "9 \'15",
            "10 \'15",
            "11 \'15",
            "12 \'15"
    };
    private final float[][] mValues = {
            {
                    4.7f, 4.3f, 8f, 6.5f, 9.9f, 7f, 8.3f, 7.0f, 1.2f, 2.2f, 2.3f, 5f,
            },
            {4.5f, 2.5f, 2.5f, 9f, 4.5f, 9.5f, 5f, 8.3f, 1.8f,1.2f,2.2f,2.3f}};

    boolean isConnected;
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("plot")) {
//                intent.getD
                Log.d(LOG_TAG, "received");
                mSymbol = intent.getStringExtra("symbol");
                Bundle args = new Bundle();
                args.putString("symbol", mSymbol);
                getLoaderManager().initLoader(CURSOR_LOADER_ID, args, StockDetailActivity.this);

//                setData(mDates, mValues);
//                    mChart.centerViewTo(mDates.get());
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null) {
            //Network Stuff
            ConnectivityManager cm =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                          activeNetwork.isConnectedOrConnecting();
            Intent mServiceIntent = new Intent(this, StockIntentService.class);
            mServiceIntent.putExtra("tag", "chart");
            mServiceIntent.putExtra("symbol", getIntent().getStringExtra("symbol"));
            setTitle(getIntent().getStringExtra("symbol").toUpperCase());
            LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("plot");
            bManager.registerReceiver(bReceiver, intentFilter);
            if (isConnected) {
                startService(mServiceIntent);
            }
            setContentView(R.layout.activity_detail);
            mChart = (LineChartView) findViewById(R.id.chart);
        }

        else{
            mSymbol = savedInstanceState.getString("symbol");
            Bundle args = new Bundle();
            args.putString("symbol", mSymbol);
            setContentView(R.layout.activity_detail);
            mChart = (LineChartView) findViewById(R.id.chart);
            getLoaderManager().initLoader(CURSOR_LOADER_ID, args, this);
        }


//      else {
//            findViewById(R.id.layout_chart).setVisibility(View.INVISIBLE);
//            findViewById(R.id.view_stock_empty).setVisibility(View.VISIBLE);
//        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.History.CONTENT_URI,
                                null,
                                HistColumns.SYMBOL + " = ?",
                                new String[]{args.getString("symbol","ERROR")},
                                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToLast()) {
            final LineSet lineSet = new LineSet();
            final int rowDateId = data.getColumnIndex(HistColumns.DATE);
            final int rowValueId = data.getColumnIndex(HistColumns.VALUE);
            float min, max,val;
            min = Float.MAX_VALUE;
            max = Float.MIN_VALUE;
            final DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                    .appendMonthOfYear(1)
                    .appendLiteral('-')
                    .appendTwoDigitYear(2000)
                    .toFormatter();
            do {
                DateTime dt = new DateTime(data.getString(rowDateId));
                Log.d(LOG_TAG, "formatted date " + dt.toString(fmt));
                val = data.getFloat(rowValueId);
                lineSet.addPoint(dt.toString(fmt),
                                 val);
                min = Math.min(min, val);
                max = Math.max(max,val);
            }
            while (data.moveToPrevious());
//            LineSet dataset = new LineSet(mLabels, mValues[0]);

            lineSet.setColor(ContextCompat.getColor(mContext, R.color.material_blue_700))
                    .setDotsColor(ContextCompat.getColor(mContext, R.color.material_blue_900))
                    .setThickness(4);
//                        .setDashed(new float[]{10f, 10f})
//                        .beginAt(5);

            mChart.addData(lineSet);
            Paint paint= new Paint();
            paint.setColor(ContextCompat.getColor(mContext,R.color.md_divider_black));
            mChart.setBorderSpacing(Tools.fromDpToPx(5))
//                    .
                    .setAxisBorderValues(Math.round(min) - 1, Math.round(max) + 1)
                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
                    .setLabelsColor(ContextCompat.getColor(mContext, R.color.material_blue_500)).setStep((max-min)>100?Math.round((max-min))/10:5)
                    .setXAxis(false)
                    .setAxisColor(ContextCompat.getColor(mContext, R.color.material_blue_700))
                            .setGrid(ChartView.GridType.FULL,paint)
//                    .setLabelsFormat()
                    .setYAxis(false);
            mChart.show();

        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader)

    {
//        mChart
        mChart.dismiss();
//        mChart.update
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("symbol",mSymbol);
//        mChart.dismiss();
//        mChart.destroyDrawingCache();
//        mChart.
        super.onSaveInstanceState(outState);
    }
}

