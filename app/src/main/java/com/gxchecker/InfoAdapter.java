package com.gxchecker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gxchecker.Entity.UggData;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class InfoAdapter extends ArrayAdapter<UggData> {

    private int resourceId;
    @BindView(R.id.info_name)
    TextView name;
    @BindView(R.id.info_value)
    TextView value;
    public InfoAdapter(@NonNull Context context, int resource, @NonNull List<UggData> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        UggData model = getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
//        name.setText(UggData.getName());
        return view;
    }
}
