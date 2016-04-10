
package com.sam_chordas.android.stockhawk.json_pojo.multi_stocks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class QueryMulti{

    @SerializedName("query")
    @Expose
    private Query_ query;

    /**
     * 
     * @return
     *     The query
     */
    public Query_ getQuery() {
        return query;
    }

    /**
     * 
     * @param query
     *     The query
     */
    public void setQuery(Query_ query) {
        this.query = query;
    }

}
