package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.google.gson.GsonBuilder;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.QueryMulti;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.Query_;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.Quote;
import com.sam_chordas.android.stockhawk.json_pojo.multi_stocks.Results;
import com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.Query;
import com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.QuerySingular;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.retrofit.QuoteFetchService;
import com.sam_chordas.android.stockhawk.retrofit.QuotesFetchService;
import com.sam_chordas.android.stockhawk.ui.StocksActivity;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

import static com.sam_chordas.android.stockhawk.rest.Utils.updateWidgets;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

//    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate, isInit, isAdd;
    private StringBuilder symbols;


    final Retrofit retrofit = new Retrofit.Builder().baseUrl(
            "https://query.yahooapis.com").addConverterFactory(
            GsonConverterFactory.create(new GsonBuilder().disableHtmlEscaping().create())).build();
    static  HttpUrl url = HttpUrl.parse("http://ichart.finance.yahoo.com/table.csv");
    private final OkHttpClient client = new OkHttpClient();

    public StockTaskService() {
    }



    public StockTaskService(Context context) {
        mContext = context;
    }
//    TODO clean up, refactor
    private void insertData(String url) throws GSMFail{
        final Map<String, String> options = new HashMap<>();
        options.put("format", "json");
        options.put("env", "store://datatables.org/alltableswithkeys");
        QuoteFetchService quoteFetchService = retrofit.create(QuoteFetchService.class);
        Call<QuerySingular> call = quoteFetchService.queryList(url, options);
        Log.d(LOG_TAG, "INSERTING data");
        call.enqueue(new Callback<QuerySingular>() {

                         @Override
                         public void onResponse(
                                 retrofit.Response<QuerySingular> response,
                                 Retrofit retrofit) {
                             QuerySingular queries = response.body();
                             if (queries != null) {
                                 Query query = queries.getQuery();
                                 Log.d(LOG_TAG, "INSERT: date: " + query.getCreated());
                                 com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.Results results = query.getResults();
                                 com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.Quote quote = results.getQuote();
                                 Log.d(LOG_TAG, "insert" + quote.getSymbol());
                                 if (quote.getAsk() == null) {
                                     Log.d(LOG_TAG, "Stock DNE");
                                     Intent RTReturn = new Intent(StocksActivity.INVALID);
                                     LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                                             RTReturn);
                                     return;
                                 }
                                 ContentValues value = new ContentValues();
                                 value.put(QuoteColumns.SYMBOL, quote.getSymbol());
                                 value.put(QuoteColumns.BIDPRICE,
                                           Utils.truncateBidPrice(quote.getBid()));
                                 value.put(QuoteColumns.PERCENT_CHANGE, Utils.truncateChange(
                                         quote.getChangeinPercent(), true));
                                 value.put(QuoteColumns.ISUP,
                                           quote.getChange().charAt(0) == '-' ? 0 : 1);
                                 value.put(QuoteColumns.CHANGE,
                                           Utils.truncateChange(quote.getChange(), false));
                                 value.put(QuoteColumns.ISCURRENT, 1);
                                 Uri s = mContext.getContentResolver().insert(
                                         QuoteProvider.Quotes.CONTENT_URI, value);
                                 Log.d(LOG_TAG, s.toString());
                                 Utils.updateWidgets(mContext);

//                                 result[0] = GcmNetworkManager.RESULT_SUCCESS;
                             }
                         }

                         @Override
                         public void onFailure(Throwable t) {
                             try {
                                 throw new GSMFail();
                             } catch (GSMFail gsmFail) {
                                 gsmFail.printStackTrace();
                             }
                         }
                     }
        );
        updateWidgets(mContext);
//        return result[0];
    }

    void updateData(String url, final boolean isInit) throws GSMFail{
        final Map<String, String> options = new HashMap<>();
        options.put("format", "json");
        options.put("env", "store://datatables.org/alltableswithkeys");
        Log.d(LOG_TAG, "UPDATE");
        QuotesFetchService quotesFetchService = retrofit.create(QuotesFetchService.class);
        Call<QueryMulti> call = quotesFetchService.queryList(url, options);
        call.enqueue(new Callback<QueryMulti>() {

            @Override
            public void onFailure(Throwable t) {
                try {
                    throw new GSMFail();
                } catch (GSMFail gsmFail) {
                    gsmFail.printStackTrace();
                }
            }


            @Override
            public void onResponse(
                    retrofit.Response<QueryMulti> response,
                    Retrofit retrofit) {
                QueryMulti queries = response.body();
                if (queries != null) {
                    Query_ query = queries.getQuery();
                    Log.d(LOG_TAG, query.getCreated() + "");
                    Results results = query.getResults();
                    int numOfQuotes = results.getQuote().size();
                    Log.d(LOG_TAG, numOfQuotes + "num of q");
                    ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();
                    if (isInit)
                        for (Quote quote : results.getQuote()) {
                            contentProviderOperations.add(ContentProviderOperation.newInsert(
                                    QuoteProvider.Quotes.withSymbol(quote.getSymbol()))
                                                                  .withValue(QuoteColumns.SYMBOL,
                                                                             quote.getSymbol())
                                                                  .withValue(QuoteColumns.BIDPRICE,
                                                                             Utils.truncateBidPrice(
                                                                                     quote.getBid()))
                                                                  .withValue(
                                                                          QuoteColumns.PERCENT_CHANGE,
                                                                          Utils.truncateChange(
                                                                                  quote.getChangeinPercent(),
                                                                                  true))
                                                                  .withValue(QuoteColumns.ISUP,
                                                                             quote.getChange().charAt(
                                                                                     0) == '-' ? 0
                                                                                               : 1).withValue(
                                            QuoteColumns.CHANGE,
                                            Utils.truncateChange(quote.getChange(),
                                                                 false)).withValue(
                                            QuoteColumns.ISCURRENT, 1).build());
                            Log.d("Initing", quote.getSymbol());
                        }
                    else
                        for (Quote quote : results.getQuote()) {
                            contentProviderOperations.add(ContentProviderOperation.newUpdate(
                                    QuoteProvider.Quotes.withSymbol(quote.getSymbol()))
//                                .withValue(QuoteColumns.SYMBOL, quote.getSymbol())
                                                                  .withValue(QuoteColumns.BIDPRICE,
                                                                             Utils.truncateBidPrice(
                                                                                     quote.getBid()))
                                                                  .withValue(
                                                                          QuoteColumns.PERCENT_CHANGE,
                                                                          Utils.truncateChange(
                                                                                  quote.getChangeinPercent(),
                                                                                  true))
                                                                  .withValue(QuoteColumns.ISUP,
                                                                             quote.getChange().charAt(
                                                                                     0) == '-' ? 0
                                                                                               : 1).withValue(
                                            QuoteColumns.CHANGE,
                                            Utils.truncateChange(quote.getChange(),
                                                                 false)).withValue(
                                            QuoteColumns.ISCURRENT, 1).build());
                            Log.d("Updating", quote.getSymbol());
                        }
                    try {
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                                                                 contentProviderOperations);
                    } catch (RemoteException | OperationApplicationException e) {
                        e.printStackTrace();
                    }
                }
                updateWidgets(mContext);
            }
        });
    }



    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        String urlString;
        symbols = new StringBuilder();
        StringBuilder urlStringBuilder = new StringBuilder();

        try {

            // Base URL for the Yahoo query
//      urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(
                    URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                                      + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                                  new String[]{"Distinct " + QuoteColumns.SYMBOL},
                                                                  null,
                                                                  null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
//                isUpdate = false;
                isInit = true;

                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                    symbols.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                    Log.d(LOG_TAG, "init cursor is empty, running init");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                isUpdate = true;
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"").append(initQueryCursor.getString(
                            initQueryCursor.getColumnIndex(
                                    "symbol"))).append("\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                    symbols.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isAdd = true;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
                symbols.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        urlString = urlStringBuilder.toString();
        try {
            urlString = URLDecoder.decode(urlString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, urlString);
        if(isAdd)
        {
            try {
                insertData(urlString);
            } catch (GSMFail gsmFail) {
            return GcmNetworkManager.RESULT_FAILURE;
        }

        }
        try {
            updateData(urlString, isInit);
        } catch (GSMFail gsmFail) {
            return GcmNetworkManager.RESULT_FAILURE;
        }
        //      try{
//        getResponse = fetchData(urlString);
//        try {
//          ContentValues contentValues = new ContentValues();
//          // update ISCURRENT to 0 (false) so new data is current
////          if (isUpdate){
////            contentValues.put(QuoteColumns.ISCURRENT, 0);
////            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
////                                                 null, null);
//
//
////          }
////          try{
//          Log.d(LOG_TAG,isUpdate+"");
//          if(isUpdate){
//
//          }
//          if( Utils.quoteJsonToContentVals(getResponse,isUpdate)!= null)
//
//              mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
//              Utils.quoteJsonToContentVals(getResponse,isUpdate));
//          else{
//        }
//        }catch (RemoteException | OperationApplicationException e){
//          Log.e(LOG_TAG, "Error applying batch insert", e);
////        }
//      } catch (IOException e){
//        e.printStackTrace();
//      }
//    }
        return GcmNetworkManager.RESULT_SUCCESS;
    }




    private class GSMFail extends Exception {
    }
}