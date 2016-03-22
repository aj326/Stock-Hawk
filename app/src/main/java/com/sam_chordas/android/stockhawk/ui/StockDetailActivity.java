package com.sam_chordas.android.stockhawk.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;

/**
 * Created by ahmed on 3/12/16.
 */
public class StockDetailActivity extends AppCompatActivity implements View.OnTouchListener {
    private static final int SERIES_SIZE = 200;
    private final String LOG_TAG = StockDetailActivity.class.getSimpleName();

    private XYPlot mySimpleXYPlot;
//    XYPlot x mx;
    private Button resetButton;
    private SimpleXYSeries series = null;
    private Context mContext;
    private PointF minXY;
    private PointF maxXY;
    boolean isConnected;
    private ArrayList<String> mDates, mValues;
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("plot")) {
                Log.d(LOG_TAG,"received");
                    mDates = new ArrayList<>(intent.getStringArrayListExtra("date"));
                    mValues = new ArrayList<>(intent.getStringArrayListExtra("val"));
                mySimpleXYPlot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
                //mySimpleXYPlot.disableAllMarkup();
                series = new SimpleXYSeries("Date");
                for(int i = 0; i < mDates.size(); i++) {
                    series.addFirst(i, Float.parseFloat(mValues.get(i)));
                }
                MyIndexFormat mif = new MyIndexFormat ();
                String dates[] = new String[mDates.size()];
                mDates.toArray(dates);
                mif.Labels = dates;
                mySimpleXYPlot.getGraphWidget().setRangeValueFormat(
                        new DecimalFormat("#####"));
                mySimpleXYPlot.getGraphWidget().setDomainValueFormat(
                        mif);

                mySimpleXYPlot.addSeries(series,
                                         new LineAndPointFormatter(Color.rgb(0, 0, 0), null,
                                                                   Color.rgb(0, 0, 150), null));
                mySimpleXYPlot.calculateMinMaxVals();


                minXY = new PointF(mySimpleXYPlot.getCalculatedMinX().floatValue(),
                                   mySimpleXYPlot.getCalculatedMinY().floatValue());
                maxXY = new PointF(mySimpleXYPlot.getCalculatedMaxX().floatValue(),
                                   mySimpleXYPlot.getCalculatedMaxY().floatValue());

                Log.d(LOG_TAG,
                      mDates.size() + " " + mValues.size() + " ");
//                    setWilliamData(mDates, mValues);

                mySimpleXYPlot.redraw();



//                    mChart.show();
//                setData(mDates, mValues);
//                    mChart.centerViewTo(mDates.get());
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_detail);
        //Network Stuff

            mContext = this;
            ConnectivityManager cm =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                          activeNetwork.isConnectedOrConnecting();

        Intent mServiceIntent = new Intent(this, StockIntentService.class);
        mServiceIntent.putExtra("tag", "chart");
        mServiceIntent.putExtra("symbol", getIntent().getStringExtra("symbol"));
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("plot");
        bManager.registerReceiver(bReceiver, intentFilter);
        setContentView(R.layout.touch_zoom_example);

        if (isConnected)
            startService(mServiceIntent);
//      else {
//            findViewById(R.id.layout_chart).setVisibility(View.INVISIBLE);
//            findViewById(R.id.view_stock_empty).setVisibility(View.VISIBLE);
//        }
        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                minXY.x = series.getX(0).floatValue();
                maxXY.x = series.getX(series.size() - 1).floatValue();
                mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
                // pre 0.5.1 users should use postRedraw() instead.
                mySimpleXYPlot.redraw();
            }
        });
        mySimpleXYPlot = (XYPlot) findViewById(R.id.plot);
        mySimpleXYPlot.setOnTouchListener(this);
        mySimpleXYPlot.getGraphWidget().setTicksPerRangeLabel(10);
        mySimpleXYPlot.getGraphWidget().setTicksPerDomainLabel(10);
        mySimpleXYPlot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);

        mySimpleXYPlot.getGraphWidget().setRangeTickLabelWidth(25);
        mySimpleXYPlot.setRangeLabel("Price");
        mySimpleXYPlot.setDomainLabel("Date");

    }

    // Definition of the touch states
    static final int NONE = 0;
    static final int ONE_FINGER_DRAG = 1;
    static final int TWO_FINGERS_DRAG = 2;
    int mode = NONE;

    PointF firstFinger;
    float distBetweenFingers;
    boolean stopThread = false;

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                stopThread = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                distBetweenFingers = spacing(event);
                // the distance check is done to avoid false alarms
                if (distBetweenFingers > 5f) {
                    mode = TWO_FINGERS_DRAG;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    scroll(oldFirstFinger.x - firstFinger.x);
                    mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x,
                                                       BoundaryMode.FIXED);
                    mySimpleXYPlot.redraw();

                } else if (mode == TWO_FINGERS_DRAG) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);
                    zoom(oldDist / distBetweenFingers);
                    mySimpleXYPlot.setDomainBoundaries(minXY.x, maxXY.x,
                                                       BoundaryMode.FIXED);
                    mySimpleXYPlot.redraw();
                }
                break;
        }
        return true;
    }

    private void zoom(float scale) {
        float domainSpan = maxXY.x - minXY.x;
        float domainMidPoint = maxXY.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;

        minXY.x = domainMidPoint - offset;
        maxXY.x = domainMidPoint + offset;

        minXY.x = Math.min(minXY.x, series.getX(series.size() - 3)
                .floatValue());
        maxXY.x = Math.max(maxXY.x, series.getX(1).floatValue());
        clampToDomainBounds(domainSpan);
    }

    private void scroll(float pan) {
        float domainSpan = maxXY.x - minXY.x;
        float step = domainSpan / mySimpleXYPlot.getWidth();
        float offset = pan * step;
        minXY.x = minXY.x + offset;
        maxXY.x = maxXY.x + offset;
        clampToDomainBounds(domainSpan);
    }

    private void clampToDomainBounds(float domainSpan) {
        float leftBoundary = series.getX(0).floatValue();
        float rightBoundary = series.getX(series.size() - 1).floatValue();
        // enforce left scroll boundary:
        if (minXY.x < leftBoundary) {
            minXY.x = leftBoundary;
            maxXY.x = leftBoundary + domainSpan;
        } else if (maxXY.x > series.getX(series.size() - 1).floatValue()) {
            maxXY.x = rightBoundary;
            minXY.x = rightBoundary - domainSpan;
        }
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.hypot(x, y);
    }
}
class MyIndexFormat extends Format {

    public String[] Labels = null;

    @Override
    public StringBuffer format(Object obj,
                               StringBuffer toAppendTo,
                               FieldPosition pos) {

        // try turning value to index because it comes from indexes
        // but if is too far from index, ignore it - it is a tick between indexes
        float fl = ((Number)obj).floatValue();
        int index = Math.round(fl);
        if(Labels == null || Labels.length <= index ||
           Math.abs(fl - index) > 0.1)
            return new StringBuffer("");

        return new StringBuffer(Labels[index]);
    }

    @Override
    public Object parseObject(String string, ParsePosition position) {
        return null;
    }
}
//public class StockDetailActivity extends AppCompatActivity  implements
//                                                            SeekBar.OnSeekBarChangeListener,
//                                                            OnChartGestureListener,
//                                                            OnChartValueSelectedListener {
//    private LineChart mChart;
//    private SeekBar mSeekBarX, mSeekBarY;
//    private TextView tvX, tvY;
//    Context mContext;
//    boolean isConnected;
//    private ArrayList<String> mDates, mValues;
//        private BroadcastReceiver bReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                if(intent.getAction().equals("plot")) {
//
//                    mDates = new ArrayList<>(intent.getStringArrayListExtra("date"));
//                    mValues = new ArrayList<>(intent.getStringArrayListExtra("val"));
//                    Log.d(LOG_TAG,
//                          mDates.size() + " " + mValues.size() + " ");
////                    setWilliamData(mDates, mValues);
//
////                    mChart.show();
//                    setData(mDates, mValues);
////                    mChart.centerViewTo(mDates.get());
//                }
//            }
//        };
//
////    private void setWilliamData(ArrayList<String> mDates, ArrayList<String> mValues) {
////        LinkedList<Float> vals = new LinkedList<>();
////        for(String val : mValues)
////        {
////            vals.add(Float.parseFloat(val));
////        }
////        float[] floatArray = new float[vals.size()];
////        int i = 0;
////
////        for (Float f : vals) {
////            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
////        }
////
////        String[] stringArray = new String[mDates.size()];
////        i = 0;
////        for (String s : mDates)
////                stringArray[i++] = (s != null? s : "");
//////        String[] d = ArrayUtils.toPrimitive((String[]) mDates.toArray());
////        LineSet dataSet = new LineSet(stringArray, floatArray);
////
////        mChart.addData(dataSet);
//////        mChart.setAxisX(false);
////        mChart.setYLabels(AxisController.LabelPosition.NONE);
////
////    }
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        setContentView(R.layout.activity_detail);
//        //Network Stuff
//        {
//            mContext = this;
//            ConnectivityManager cm =
//                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//
//            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//            isConnected = activeNetwork != null &&
//                          activeNetwork.isConnectedOrConnecting();
//        }
//        Intent mServiceIntent = new Intent(this, StockIntentService.class);
//        mServiceIntent.putExtra("tag", "chart");
//        mServiceIntent.putExtra("symbol", getIntent().getStringExtra("symbol"));
//        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("plot");
//        bManager.registerReceiver(bReceiver, intentFilter);
//        if (isConnected){
//            startService(mServiceIntent);
//        }     else {
//            findViewById(R.id.layout_chart).setVisibility(View.INVISIBLE);
//            findViewById(R.id.view_stock_empty).setVisibility(View.VISIBLE);
//        }
//        mChart = (LineChart) findViewById(R.id.chart);
////
////
////        tvX = (TextView) findViewById(R.id.tvXMax);
////        tvY = (TextView) findViewById(R.id.tvYMax);
////
////        mSeekBarX = (SeekBar) findViewById(R.id.seekBar1);
////        mSeekBarY = (SeekBar) findViewById(R.id.seekBar2);
////
//////
////        mSeekBarX.setProgress(45);
////        mSeekBarY.setProgress(100);
////
////        mSeekBarY.setOnSeekBarChangeListener(this);
////        mSeekBarX.setOnSeekBarChangeListener(this);
//
//        mChart = (LineChart) findViewById(R.id.chart);
//        mChart.setOnChartGestureListener(this);
//        mChart.setOnChartValueSelectedListener(this);
//        mChart.setDrawGridBackground(false);
//
////         no description text
//        mChart.setDescription("");
//        mChart.setNoDataTextDescription("You need to provide data for the chart.");
//
//        // enable touch gestures
//        mChart.setTouchEnabled(true);
//
//        // enable scaling and dragging
//        mChart.setDragEnabled(true);
//        mChart.setScaleEnabled(true);
//         mChart.setScaleXEnabled(true);
//         mChart.setScaleYEnabled(true);
////
////        // if disabled, scaling can be done on x- and y-axis separately
//        mChart.setPinchZoom(true);
//
//        // set an alternative background color
//         mChart.setBackgroundColor(Color.GRAY);
////
////        // create a custom MarkerView (extend MarkerView) and specify the layout
////        // to use for it
//        MyMarkerView mv = new MyMarkerView(this, R.layout.custom_marker_view);
////
////        // set the marker to the chart
//        mChart.setMarkerView(mv);
//
//        // x-axis limit line
////        LimitLine llXAxis = new LimitLine(10f, "Index 10");
////        llXAxis.setLineWidth(4f);
////        llXAxis.enableDashedLine(10f, 10f, 0f);
////        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
////        llXAxis.setTextSize(10f);
//
////        XAxis xAxis = mChart.getXAxis();
//
//        //xAxis.setValueFormatter(new MyCustomXAxisValueFormatter());
//        //xAxis.addLimitLine(llXAxis); // add x-axis limit line
//
////        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
//
////        LimitLine ll1 = new LimitLine(130f, "Upper Limit");
////        ll1.setLineWidth(4f);
////        ll1.enableDashedLine(10f, 10f, 0f);
////        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
////        ll1.setTextSize(10f);
////        ll1.setTypeface(tf);
////
////        LimitLine ll2 = new LimitLine(-30f, "Lower Limit");
////        ll2.setLineWidth(4f);
////        ll2.enableDashedLine(10f, 10f, 0f);
////        ll2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
////        ll2.setTextSize(10f);
////        ll2.setTypeface(tf);
////
////        YAxis leftAxis = mChart.getAxisLeft();
////        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
////        leftAxis.addLimitLine(ll1);
////        leftAxis.addLimitLine(ll2);
////        leftAxis.setAxisMaxValue(220f);
////        leftAxis.setAxisMinValue(-50f);
////        //leftAxis.setYOffset(20f);
////        leftAxis.enableGridDashedLine(10f, 10f, 0f);
////        leftAxis.setDrawZeroLine(false);
//
//        // limit lines are drawn behind data (and not on top)
////        leftAxis.setDrawLimitLinesBehindData(true);
//
////        mChart.getAxisRight().setEnabled(false);
//
//        //mChart.getViewPortHandler().setMaximumScaleY(2f);
//        //mChart.getViewPortHandler().setMaximumScaleX(2f);
//
//        // add data
////        setData(45, 100);
////        if(mDates!=null && mValues!=null)
////        setData(mDates,mValues);
//
//
////        mChart.setVisibleXRange(20, XAxis.XAxisPosition);
////        mChart.setVisibleYRange(20f, YAxis.AxisDependency.LEFT);
////        mChart.centerViewTo(20, 50, YAxis.AxisDependency.LEFT);
//
//        mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);
////        mChart.invalidate();
//
//        // get the legend (only possible after setting data)
////        Legend l = mChart.getLegend();
//
//        // modify the legend ...
//        // l.setPosition(LegendPosition.LEFT_OF_CHART);
////        l.setForm(Legend.LegendForm.LINE);
//
//        // // dont forget to refresh the drawing
//         mChart.invalidate();
//    }
//
//
//        @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//    }
//
////    @Override
////    public boolean onCreateOptionsMenu(Menu menu) {
////        getMenuInflater().inflate(R.menu.line, menu);
////        return true;
////    }
////
////    @Override
////    public boolean onOptionsItemSelected(MenuItem item) {
////
////        switch (item.getItemId()) {
////            case R.id.actionToggleValues: {
////                List<ILineDataSet> sets = mChart.getData()
////                        .getDataSets();
////
////                for (ILineDataSet iSet : sets) {
////
////                    LineDataSet set = (LineDataSet) iSet;
////                    set.setDrawValues(!set.isDrawValuesEnabled());
////                }
////
////                mChart.invalidate();
////                break;
////            }
////            case R.id.actionToggleHighlight: {
////                if(mChart.getData() != null) {
////                    mChart.getData().setHighlightEnabled(!mChart.getData().isHighlightEnabled());
////                    mChart.invalidate();
////                }
////                break;
////            }
////            case R.id.actionToggleFilled: {
////
////                List<ILineDataSet> sets = mChart.getData()
////                        .getDataSets();
////
////                for (ILineDataSet iSet : sets) {
////
////                    LineDataSet set = (LineDataSet) iSet;
////                    if (set.isDrawFilledEnabled())
////                        set.setDrawFilled(false);
////                    else
////                        set.setDrawFilled(true);
////                }
////                mChart.invalidate();
////                break;
////            }
////            case R.id.actionToggleCircles: {
////                List<ILineDataSet> sets = mChart.getData()
////                        .getDataSets();
////
////                for (ILineDataSet iSet : sets) {
////
////                    LineDataSet set = (LineDataSet) iSet;
////                    if (set.isDrawCirclesEnabled())
////                        set.setDrawCircles(false);
////                    else
////                        set.setDrawCircles(true);
////                }
////                mChart.invalidate();
////                break;
////            }
////            case R.id.actionToggleCubic: {
////                List<ILineDataSet> sets = mChart.getData()
////                        .getDataSets();
////
////                for (ILineDataSet iSet : sets) {
////
////                    LineDataSet set = (LineDataSet) iSet;
////                    if (set.isDrawCubicEnabled())
////                        set.setDrawCubic(false);
////                    else
////                        set.setDrawCubic(true);
////                }
////                mChart.invalidate();
////                break;
////            }
////            case R.id.actionToggleStepped: {
////                List<ILineDataSet> sets = mChart.getData()
////                        .getDataSets();
////
////                for (ILineDataSet iSet : sets) {
////
////                    LineDataSet set = (LineDataSet) iSet;
////                    if (set.isDrawSteppedEnabled())
////                        set.setDrawStepped(false);
////                    else
////                        set.setDrawStepped(true);
////                }
////                mChart.invalidate();
////                break;
////            }
////            case R.id.actionTogglePinch: {
////                if (mChart.isPinchZoomEnabled())
////                    mChart.setPinchZoom(false);
////                else
////                    mChart.setPinchZoom(true);
////
////                mChart.invalidate();
////                break;
////            }
////            case R.id.actionToggleAutoScaleMinMax: {
////                mChart.setAutoScaleMinMaxEnabled(!mChart.isAutoScaleMinMaxEnabled());
////                mChart.notifyDataSetChanged();
////                break;
////            }
////            case R.id.animateX: {
////                mChart.animateX(3000);
////                break;
////            }
////            case R.id.animateY: {
////                mChart.animateY(3000, Easing.EasingOption.EaseInCubic);
////                break;
////            }
////            case R.id.animateXY: {
////                mChart.animateXY(3000, 3000);
////                break;
////            }
////            case R.id.actionSave: {
////                if (mChart.saveToPath("title" + System.currentTimeMillis(), "")) {
////                    Toast.makeText(getApplicationContext(), "Saving SUCCESSFUL!",
////                                   Toast.LENGTH_SHORT).show();
////                } else
////                    Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
////                            .show();
////
////                // mChart.saveToGallery("title"+System.currentTimeMillis())
////                break;
////            }
////        }
////        return true;
////    }
//
//    @Override
//    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
////
//        if (!fromUser) return;
//        tvX.setText((mSeekBarX.getProgress() + 1));
//        tvY.setText((mSeekBarY.getProgress()));
////
//        mChart.centerViewTo(mSeekBarX.getProgress(), mSeekBarY.getProgress(), YAxis.AxisDependency.LEFT);
////
////        // redraw
////        mChart.invalidate();
//    }
//
//    @Override
//    public void onStartTrackingTouch(SeekBar seekBar) {
//        // TODO Auto-generated method stub
//
//    }
//
//    @Override
//    public void onStopTrackingTouch(SeekBar seekBar) {
//        // TODO Auto-generated method stub
//
//    }
//    private void setData(ArrayList<String> dates, ArrayList<String> values) {
////        mSeekBarX.setMax(dates.size());
////        mSeekBarY.setMax(values.size());
//
//        ArrayList<Entry> yVals = new ArrayList<Entry>(values.size());
//        for(int i= 0;i<yVals.size(); i++){
//            yVals.add(new Entry(Float.parseFloat(values.get(i)), i));
//        }
//        LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
//        LineData data = new LineData(dates, set1);
//        mChart.setData(data);
//        mChart.invalidate();
//
//        // set1.setFillAlpha(110);
//        // set1.setFillColor(Color.RED);
//
//        // set the line to be drawn like this "- - - - - -"
////        set1.enableDashedLine(10f, 5f, 0f);
////        set1.enableDashedHighlightLine(10f, 5f, 0f);
////        set1.setColor(Color.BLACK);
////        set1.setCircleColor(Color.BLACK);
////        set1.setLineWidth(1f);
////        set1.setCircleRadius(3f);
////        set1.setDrawCircleHole(false);
////        set1.setValueTextSize(9f);
////        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
////        set1.setFillDrawable(drawable);
////        set1.setDrawFilled(true);
////
////        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
////        dataSets.add(set1);
////        LineData data = new LineData(values, dataSets);
////        mChart.setData(data);
//    }
////
////
//    private void setData(int count, float range) {
//
//        ArrayList<String> xVals = new ArrayList<String>();
//        for (int i = 0; i < count; i++) {
//            xVals.add((i) + "");
//        }
//
//        ArrayList<Entry> yVals = new ArrayList<Entry>();
//
//        for (int i = 0; i < count; i++) {
//
//            float mult = (range + 1);
//            float val = (float) (Math.random() * mult) + 3;// + (float)
//            // ((mult *
//            // 0.1) / 10);
//            yVals.add(new Entry(val, i));
//        }
//
//        // create a dataset and give it a type
//        LineDataSet set1 = new LineDataSet(yVals, "DataSet 1");
//        // set1.setFillAlpha(110);
//        // set1.setFillColor(Color.RED);
//
//        // set the line to be drawn like this "- - - - - -"
//        set1.enableDashedLine(10f, 5f, 0f);
//        set1.enableDashedHighlightLine(10f, 5f, 0f);
//        set1.setColor(Color.BLACK);
//        set1.setCircleColor(Color.BLACK);
//        set1.setLineWidth(1f);
//        set1.setCircleRadius(3f);
//        set1.setDrawCircleHole(false);
//        set1.setValueTextSize(9f);
//        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
//        set1.setFillDrawable(drawable);
//        set1.setDrawFilled(true);
//
//        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
//        dataSets.add(set1); // add the datasets
//
//        // create a data object with the datasets
//        LineData data = new LineData(xVals, dataSets);
//
//        // set data
//        mChart.setData(data);
//    }
////
//    @Override
//    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
//        Log.i("Gesture", "START, x: " + me.getX() + ", y: " + me.getY());
//    }
//
//    @Override
//    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
//        Log.i("Gesture", "END, lastGesture: " + lastPerformedGesture);
//
//        // un-highlight values after the gesture is finished and no single-tap
//        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
//            mChart.highlightValues(null); // or highlightTouch(null) for callback to onNothingSelected(...)
//    }
//
//    @Override
//    public void onChartLongPressed(MotionEvent me) {
//        Log.i("LongPress", "Chart longpressed.");
//    }
//
//    @Override
//    public void onChartDoubleTapped(MotionEvent me) {
//        Log.i("DoubleTap", "Chart double-tapped.");
//    }
//
//    @Override
//    public void onChartSingleTapped(MotionEvent me) {
//        Log.i("SingleTap", "Chart single-tapped.");
//    }
//
//    @Override
//    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
//        Log.i("Fling", "Chart flinged. VeloX: " + velocityX + ", VeloY: " + velocityY);
//    }
//
//    @Override
//    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
//        Log.i("Scale / Zoom", "ScaleX: " + scaleX + ", ScaleY: " + scaleY);
//    }
//
//    @Override
//    public void onChartTranslate(MotionEvent me, float dX, float dY) {
//        Log.i("Translate / Move", "dX: " + dX + ", dY: " + dY);
//    }
//
//    @Override
//    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
//        Log.i("Entry selected", e.toString());
//        Log.i("LOWHIGH", "low: " + mChart.getLowestVisibleXIndex() + ", high: " + mChart.getHighestVisibleXIndex());
//        Log.i("MIN MAX", "xmin: " + mChart.getXChartMin() + ", xmax: " + mChart.getXChartMax() + ", ymin: " + mChart.getYChartMin() + ", ymax: " + mChart.getYChartMax());
//    }
//
//    @Override
//    public void onNothingSelected() {
//        Log.i("Nothing selected", "Nothing selected.");
//    }
//}
