package com.technorides.practicemapbox;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by moises on 20/04/16.
 */
public class DialogListAddresses extends DialogFragment{

    public MainActivity mainActivity;
    private TextView empty;
    private ListView listAddresses;
    private ArrayAdapter<Address> adapter;
    private List<Address> list;

    public DialogListAddresses(MainActivity mainActivity, List<Address> list){
        this.mainActivity = mainActivity;
        this.list = list;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(R.string.wanted_find));
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.list_addresses, null);
        if(list.size() == 0){
            empty = (TextView)view.findViewById(R.id.empty);
            empty.setText(getResources().getString(R.string.not_found_data));
        }else{
            listAddresses = (ListView)view.findViewById(R.id.listAddresses);
            adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, list);
            listAddresses.setAdapter(adapter);
        }
        builder.setView(view);
        return builder.create();
    }
}
