package org.kore.kolabnotes.android.content;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by koni on 11.06.15.
 */
public class DataCaches {
    private final Map<AccountIdentifier, DataCache> noteCache;
    private final List<String> tags;
    private final Context context;

    public DataCaches(Context context) {
        this.noteCache = new ConcurrentHashMap<>();
        this.tags = new ArrayList<>();
        this.context = context;
    }

   public synchronized List<String> getTags(){
       if(tags.isEmpty()){
           reloadTags();
       }
       return Collections.unmodifiableList(tags);
   }

    public synchronized void reloadTags(){
        long ts = System.currentTimeMillis();
        Log.d("DataCaches - reloadTags","Start reloading tags");
        tags.clear();
        tags.addAll(new TagRepository(context).getAll());
        Log.d("DataCaches - reloadTags","Reloading finished in "+(System.currentTimeMillis()-ts)+"ms");
    }

   public DataCache getNoteCache(AccountIdentifier account){
       DataCache dataCache = noteCache.get(account);

       if(dataCache == null){
           dataCache = new DataCache(context,account);
           noteCache.put(account,dataCache);
       }

       return dataCache;
   }
}
