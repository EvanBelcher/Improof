package com.example.evbel.improof;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by evbel on 7/11/2016.
 */
public class RecordingListAdapter {
    ArrayAdapter<String> adapter;
    ArrayList<String> list;
    Context context;
    ListView listView;
    public final static String EXTRA_ITEM = "com.example.evbel.improof.ITEM";

    public RecordingListAdapter(Context context, ListView listView){
        list = new ArrayList<String>();
        this.context = context;
        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, list);
        this.listView = listView;
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AppCompatTextView tv = (AppCompatTextView) view;
                String str = tv.getText().toString();
                openDetailActivity(str);
            }
        });
        refreshList();
    }

    public ArrayList<String> getList(){
        return list;
    }

    public void refreshList(){
        File rootFile = new File(MainActivity.ROOT);
        if(rootFile.exists() && rootFile.isDirectory())
            refreshList(rootFile);
        else
            rootFile.mkdirs();
    }

    private void refreshList(File directory){
        TreeMap<Long,String> filesInDirectory = new TreeMap<Long,String>();
        long currentTime = System.currentTimeMillis();
        for(File file : directory.listFiles()){
            if(file.isDirectory())
                filesInDirectory.put(currentTime - file.lastModified(),"PROJECT: " + file.getName());
            else
                filesInDirectory.put(currentTime - file.lastModified(),file.getName().split("\\.")[0]);
        }
        list.clear();
        ArrayList<String> oldList = new ArrayList<String>(list);
        list.addAll(filesInDirectory.values());
        int i = 0;
        do {
            adapter.notifyDataSetChanged();
        }while(oldList.containsAll(list) && i++ < 10);

        Log.d("app",list.toString());
    }

    public void openDetailActivity(String item){
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(EXTRA_ITEM,item);
        context.startActivity(intent);
    }



}
