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
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.FetchPlotDataService;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * Created by ahmed on 3/12/16.
 */
public class StockDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private final String LOG_TAG = StockDetailActivity.class.getSimpleName();

    private LineChartView mChart;
    private final int CURSOR_LOADER_ID = 0;
    private String mSymbol;
    static HttpUrl url = HttpUrl.parse("http://ichart.finance.yahoo.com/table.csv");
    private final OkHttpClient client = new OkHttpClient();
    private final Context mContext = this;
    public static final String ACTION_DATA_PLOT_POINTS_GATHERED = "com.sam_chordas.android.stockhawk.app.ACTION_DATA_PLOT_POINTS_GATHERED";
    public static final String FETCH_POINTS = "com.sam_chordas.android.stockhawk.app.FETCH_POINTS";
    boolean isConnected;
    float min, max;

    final BroadcastReceiver dateFetched = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG,"received!");
            Bundle args = new Bundle();
            args.putString("symbol", mSymbol);
            getLoaderManager().restartLoader(CURSOR_LOADER_ID, args, StockDetailActivity.this);
        }

    };


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_detail);
            mChart = (LineChartView) findViewById(R.id.chart);
            isConnected = Utils.isConnected(mContext);
            mSymbol = getIntent().getData().getLastPathSegment();
            setTitle(mSymbol.toUpperCase());
            IntentFilter statusIntentFilter = new IntentFilter(
                    FetchPlotDataService.BROADCAST_ACTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    dateFetched, statusIntentFilter);
            if (isConnected) {
                Intent fetchServiceIntent = new Intent(FETCH_POINTS, getIntent().getData(),
                                                       mContext,
                                                       FetchPlotDataService.class);
                startService(fetchServiceIntent);
            }

        else {
            Bundle args = new Bundle();
            args.putString("symbol", mSymbol);
            getLoaderManager().restartLoader(CURSOR_LOADER_ID, args, this);
        }
            Bundle args = new Bundle();
            args.putString("symbol", mSymbol);
            setContentView(R.layout.activity_detail);
            mChart = (LineChartView) findViewById(R.id.chart);
            getLoaderManager().restartLoader(CURSOR_LOADER_ID, args, this);
}


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
            Log.d(LOG_TAG, "onCreateLoader -- uri: " + QuoteProvider.History.withSymbol(
                    args.getString("symbol", "ERROR")));
        return new CursorLoader(this,
                                QuoteProvider.History.withSymbol(args.getString("symbol")),
                                null,
                                HistColumns.SYMBOL + " = ?",
                                new String[]{args.getString("symbol","ERROR")},
                                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//      dismiss chart since I don't have an adapter
        mChart.dismiss();
        Log.d(LOG_TAG, "finished loading");
        if(data!=null)
        if (data.moveToLast()) {
            final LineSet lineSet = new LineSet();
            final int rowDateId = data.getColumnIndex(HistColumns.DATE);
            final int rowValueId = data.getColumnIndex(HistColumns.VALUE);
            float min, max, val;
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
                max = Math.max(max, val);
            }
            while (data.moveToPrevious());
            lineSet.setColor(ContextCompat.getColor(mContext, R.color.material_blue_700))
                    .setDotsColor(ContextCompat.getColor(mContext, R.color.material_blue_900))
                    .setThickness(4);
            mChart.addData(lineSet);
            Paint paint = new Paint();
            paint.setColor(ContextCompat.getColor(mContext, R.color.md_divider_black));
            mChart.setBorderSpacing(Tools.fromDpToPx(5))
//                    .
                    .setAxisBorderValues(Math.round(min) - 1, Math.round(max) + 1)
                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
                    .setLabelsColor(
                            ContextCompat.getColor(mContext, R.color.material_blue_500)).setStep(
                    (max - min) > 100 ? Math.round((max - min)) / 10 : 5)
                    .setXAxis(false)
                    .setAxisColor(ContextCompat.getColor(mContext, R.color.material_blue_700))
                    .setGrid(ChartView.GridType.FULL, paint)
                    .setYAxis(false);
            mChart.show();
        }
    }


    @Override
    public void onLoaderReset(Loader < Cursor > loader) {
            Log.d(LOG_TAG, "resetting loader ... setting chart to null");
        mChart.dismiss();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dateFetched);
    }
}

