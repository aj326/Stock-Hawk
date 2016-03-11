package com.sam_chordas.android.stockhawk.retrofit;

import com.sam_chordas.android.stockhawk.json_pojo.singular_stocks.QuerySingular;

import java.util.Map;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;
import retrofit.http.QueryMap;


/**
 * Created by ahmed on 3/11/16.
 */
public interface QuoteFetchService {
    @GET("/v1/public/yql")
    Call<QuerySingular> queryList(
            @Query("q") String query,
            @QueryMap Map<String, String> options);
}
