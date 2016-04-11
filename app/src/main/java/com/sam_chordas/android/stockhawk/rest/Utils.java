package com.sam_chordas.android.stockhawk.rest;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();
  public static final String ACTION_DATA_UPDATED = "com.sam_chordas.android.stockhawk.app.ACTION_DATA_UPDATED";


  public static boolean showPercent = true;

  public static void errorToast(Activity activity,String error){
    Toast toast =
            Toast.makeText(activity, error,
                           Toast.LENGTH_LONG);
    toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
    toast.show();
    return;
  }
  public static boolean isConnected(Context context){
    ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null &&
                  activeNetwork.isConnectedOrConnecting();
  }
  @SuppressWarnings("ResourceType")
  static public @QuoteErrorHandling
  int getQuoteStatus(Context c){
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    return sp.getInt(c.getString(R.string.pref_quote_status_key), QuoteErrorHandling.QUOTE_STATUS_UNKNOWN);
  }
  public static void updateWidgets(Context context) {
//        Context context = mContext;
    // Setting the package ensures that only components in our app will receive the broadcast
    Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
            .setPackage(context.getPackageName());
    context.sendBroadcast(dataUpdatedIntent);
  }

//  @Retention(RetentionPolicy.SOURCE)
//  @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID,  LOCATION_STATUS_UNKNOWN, LOCATION_STATUS_INVALID})
//  public @interface LocationStatus {}
//
//  public static final int LOCATION_STATUS_OK = 0;
//  public static final int LOCATION_STATUS_SERVER_DOWN = 1;
//  public static final int LOCATION_STATUS_SERVER_INVALID = 2;
//  public static final int LOCATION_STATUS_UNKNOWN = 3;
//  public static final int LOCATION_STATUS_INVALID = 4;

  public static ArrayList quoteJsonToContentVals(String JSON, boolean isUpdate){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject;
    JSONArray resultsArray;
    Log.i(LOG_TAG, "GET FB: " +JSON);
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        //check if the only stock is valid
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          Log.d("Ask",jsonObject.getString("Ask"));
          if(jsonObject.getString("Ask").equals("null")){
            Log.d(LOG_TAG,"Returning Null");
            return null;
          }
          //new insert
          batchOperations.add(buildBatchOperation(jsonObject,isUpdate));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject,isUpdate));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuilder changeBuffer = new StringBuilder(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, boolean isUpdate){
    ContentProviderOperation.Builder builder = isUpdate? ContentProviderOperation.newUpdate(
        QuoteProvider.Quotes.CONTENT_URI): ContentProviderOperation.newInsert(QuoteProvider.Quotes.CONTENT_URI);
    try {
      Log.d(LOG_TAG,"buildBatchOp"+jsonObject.getString("symbol"));
      String change = jsonObject.getString("Change");

      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }

    return builder.build();
  }
}
