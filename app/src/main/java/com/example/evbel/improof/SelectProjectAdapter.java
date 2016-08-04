package com.example.evbel.improof;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by evbel on 7/12/2016.
 */
public class SelectProjectAdapter {

    Context context;
    Spinner spinner;
    ArrayList<String> list;
    ArrayAdapter<String> adapter;



    String selectedProject;

    public SelectProjectAdapter(Context context, Spinner spinner) {
        list = new ArrayList<String>();
        this.context = context;
        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, list);
        this.spinner = spinner;
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                AppCompatTextView tv = (AppCompatTextView) view;
                selectedProject = tv.getText().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        refreshList();
    }

    public void refreshList() {
        list.clear();
        list.add("");
        File directory = new File(MainActivity.ROOT);
        TreeMap<Long,String> filesInDirectory = new TreeMap<Long,String>();
        long currentTime = System.currentTimeMillis();
        for(File file : directory.listFiles()){
            if(file.isDirectory())
                filesInDirectory.put(currentTime - file.lastModified(),file.getName());
        }
        ArrayList<String> oldList = new ArrayList<String>(list);
        list.addAll(filesInDirectory.values());
        int i = 0;
        do {
            adapter.notifyDataSetChanged();
        }while(oldList.containsAll(list) && i++ < 10);
        if (list.size() >= 2)
            spinner.setSelection(1);
    }

    public String getSelectedProject() {
        return selectedProject;
    }
}
