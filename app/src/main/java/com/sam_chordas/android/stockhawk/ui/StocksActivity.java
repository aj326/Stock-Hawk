package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.stetho.Stetho;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.QuoteErrorHandling;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import static com.sam_chordas.android.stockhawk.rest.Utils.updateWidgets;

public class StocksActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private String message;
    private RecyclerView mRecyclerView;
    boolean isConnected;
    private SwipeRefreshLayout swipeContainer;
    public static final String INVALID = "com.sam_chordas.android.stockhawk.app.INVALID_STOCK";
    public static final String MAIN_TO_DETAIL = "com.sam_chordas.android.stockhawk.app.MAIN_TO_DETAIL";
    private boolean isInit;
    private String LOG_TAG = this.getClass().getSimpleName();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                        .build());
        Log.d(LOG_TAG,"onCreate");

        mContext = this;
        isConnected = Utils.isConnected(mContext);
        setContentView(R.layout.activity_stocks);
        mServiceIntent = new Intent(this, StockIntentService.class);
        IntentFilter invalidIntentFilter = new IntentFilter();
        invalidIntentFilter.addAction(INVALID);
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        bManager.registerReceiver(bReceiver, invalidIntentFilter);
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        //first time creation
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            if (isConnected) {
                isInit = true;
                mServiceIntent.putExtra("tag", "init");
                startService(mServiceIntent);
            }
            getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
            Log.d(LOG_TAG, "initLoader");
        }
            else {
                Log.d(LOG_TAG, "restartLoader");
                getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

            }


            mCursorAdapter = new QuoteCursorAdapter(mContext,null);
            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(mCursorAdapter);
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.attachToRecyclerView(mRecyclerView);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isConnected) {
                        new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                                .content(R.string.content_test)
                                .inputType(InputType.TYPE_CLASS_TEXT)
                                .input(R.string.input_hint, R.string.input_prefill,
                                       new MaterialDialog.InputCallback() {
                                           @Override
                                           public void onInput(
                                                   MaterialDialog dialog,
                                                   CharSequence input) {
                                               String inputUC = input.toString().toUpperCase();
                                               // On FAB click, receive user input. Make sure the stock doesn't already exist
                                               // in the DB and proceed accordingly
                                               Cursor c = getContentResolver().query(
                                                       QuoteProvider.Quotes.CONTENT_URI,
                                                       new String[]{QuoteColumns.SYMBOL},
                                                       QuoteColumns.SYMBOL + "= ?",
                                                       new String[]{inputUC}, null);
                                               if (c.getCount() != 0) {
                                                   Utils.errorToast(StocksActivity.this,
                                                                    inputUC.concat(" is already saved!"));
                                                   return;
                                               } else {
                                                   // Add the stock to DB
                                                   mServiceIntent.putExtra("tag", "add");
                                                   mServiceIntent.putExtra("symbol",
                                                                           inputUC);
                                                   startService(mServiceIntent);
                                               }
                                               c.close();
                                           }
                                       })
                                .show();

                    } else {
                        networkToast();
                    }

                }
            });

            mTitle = getTitle();
            if (isConnected) {
                long period = 3600L;
                long flex = 10L;
                String periodicTag = "periodic";
                // create a periodic task to pull stocks once every hour after the app has been opened. This
                // is so Widget data stays up to date.
                PeriodicTask periodicTask = new PeriodicTask.Builder()
                        .setService(StockTaskService.class)
                        .setPeriod(period)
                        .setFlex(flex)
                        .setTag(periodicTag)
                        .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                        .setRequiresCharging(false)
                        .build();
                // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
                // are updated.
                GcmNetworkManager.getInstance(this).schedule(periodicTask);
            }


        }



    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
            updateWidgets(mContext);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                                null,
                                null,
                                null,
                                null);


    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG,"onLoadFinished");
        mCursorAdapter.swapCursor(data);
//        mCursorAdapter.notifyDataSetChanged();
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
//        data.moveToPosition()
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                                                                               new RecyclerViewItemClickListener.OnItemClickListener() {
                                                                                   @Override
                                                                                   public void onItemClick(
                                                                                           View v,
                                                                                           int position) {
                                                                                       Log.d(LOG_TAG, position+"");
                                                                                       Intent detailActivityIntent = new Intent(
                                                                                               MAIN_TO_DETAIL,
                                                                                               QuoteProvider.History.withSymbol(
                                                                                                       mCursorAdapter.getSymbol(
                                                                                                               position)),
                                                                                               mContext,
                                                                                               ChartActivity.class);
                                                                                       v.setContentDescription(
                                                                                               "Plot: " + mCursorAdapter.getSymbol(
                                                                                                       position));
                                                                                       startActivity(
                                                                                               detailActivityIntent);
                                                                                   }
                                                                               }));
        if (!isInit && data.getCount() == 0) {
          updateEmptyView();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INVALID)) {
                Utils.errorToast(StocksActivity.this, getString(R.string.stock_dne));
            }
        }
    };

    public void updateEmptyView() {
        if (mCursorAdapter.getItemCount() == 0) {
            findViewById(R.id.view_stock_empty).setVisibility(View.VISIBLE);
            findViewById(R.id.recycler_view).setVisibility(View.INVISIBLE);
            int message = R.string.empty_stock_list;
            @QuoteErrorHandling int stat = Utils.getQuoteStatus(mContext);
            switch (stat) {
                case QuoteErrorHandling.QUOTE_STATUS_SERVER_DOWN:
                    message = R.string.empty_stock_list_server_down;
                    break;
                case QuoteErrorHandling.QUOTE_STATUS_SERVER_INVALID:
                    message = R.string.empty_stock_list_server_error;
                    break;
                default:
                    if(!isConnected)
                    message = R.string.empty_stock_list_no_network;
            }
            ((TextView) findViewById(R.id.view_stock_empty)).setText(message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
