package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by ahmed on 3/12/16.
 */
public class StockDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private final String LOG_TAG = StockDetailActivity.class.getSimpleName();

    private LineChartView mChart;
    private final int CURSOR_LOADER_ID = 0;
    private String mSymbol;
    static  HttpUrl url = HttpUrl.parse("http://ichart.finance.yahoo.com/table.csv");
    private final OkHttpClient client = new OkHttpClient();
    private final Context mContext = this;
    public static final String ACTION_DATA_PLOT_POINTS_GATHERED = "com.sam_chordas.android.stockhawk.app.ACTION_DATA_PLOT_POINTS_GATHERED";
    public static final String FETCH_POINTS = "com.sam_chordas.android.stockhawk.app.FETCH_POINTS";
    boolean isConnected;
    final ArrayList<String> dates = new ArrayList<String>();
    final ArrayList<Float> values = new ArrayList<Float>();
    final LinkedHashMap<String,Float> map =  new LinkedHashMap<>();

    float min, max;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mChart = (LineChartView) findViewById(R.id.chart);
        mChart.dismiss();
        isConnected = Utils.isConnected(mContext);

            if (isConnected)
            {
                Intent fetchServiceIntent = new Intent(FETCH_POINTS,getIntent().getData(),mContext,
                                                       FetchPlotDataService.class);
                startService(fetchServiceIntent);

            }


        mSymbol = getIntent().getData().getLastPathSegment();
        setTitle(mSymbol.toUpperCase());
            Bundle args = new Bundle();
            args.putString("symbol", mSymbol);
            getLoaderManager().initLoader(CURSOR_LOADER_ID, args, this);
        }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.History.withSymbol(args.getString("symbol","ERROR")),
                                null,
                                null,
                                null,
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
            lineSet.setColor(ContextCompat.getColor(mContext, R.color.material_blue_700))
                    .setDotsColor(ContextCompat.getColor(mContext, R.color.material_blue_900))
                    .setThickness(4);

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
        mChart.dismiss();
    }


    @Override
    protected void onDestroy() {
        mChart.dismiss();
        super.onDestroy();
    }
}

