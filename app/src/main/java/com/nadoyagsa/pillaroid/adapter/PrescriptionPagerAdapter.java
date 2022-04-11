package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.data.PrescriptionInfo;

import java.util.ArrayList;

public class PrescriptionPagerAdapter extends RecyclerView.Adapter<PrescriptionPagerAdapter.ResultViewHolder> {
    private Context context;
    private ArrayList<PrescriptionInfo> resultList;

    public PrescriptionPagerAdapter(ArrayList<PrescriptionInfo> resultList) {
        this.resultList = resultList;
    }

    @NonNull
    @Override
    public PrescriptionPagerAdapter.ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_prescription_result, parent, false);
        PrescriptionPagerAdapter.ResultViewHolder viewHolder = new PrescriptionPagerAdapter.ResultViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PrescriptionPagerAdapter.ResultViewHolder holder, int position) {
        holder.medicineName.setText(resultList.get(position).getMedicineName());
        if (resultList.get(position).getIsFavorites())
            holder.ibtFavorites.setImageResource(R.drawable.icon_star_on);
        else
            holder.ibtFavorites.setImageResource(R.drawable.icon_star_off);

        //TODO: 알약이 아닐 경우 코드 추가
        /* 알약이 아닐 경우에는 llPillForm의 visibility를 gone 해야 함! */
        holder.pillShape.setText(resultList.get(position).getPillShape());
        holder.pillDivision.setText(resultList.get(position).getPillDivision());
        holder.pillFormulation.setText(resultList.get(position).getPillFormulation());

        holder.classification.setText(resultList.get(position).getClassification());

        holder.efficacy1.setText(resultList.get(position).getEfficacy1());
        holder.efficacy2.setText(resultList.get(position).getEfficacy2());
    }

    @Override
    public int getItemCount() { return resultList.size(); }

    public class ResultViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton ibtFavorites;
        LinearLayout llPillForm;
        TextView medicineName, pillShape, pillDivision, pillFormulation, classification, efficacy1, efficacy2;

        ResultViewHolder(final View itemView) {
            super(itemView);

            medicineName = itemView.findViewById(R.id.tv_item_prescription_medicine_name);
            ibtFavorites = itemView.findViewById(R.id.ibt_item_prescription_favorites);
            llPillForm = itemView.findViewById(R.id.ll_item_prescription_pill_form);
            pillShape = itemView.findViewById(R.id.tv_item_prescription_pill_shape);
            pillDivision = itemView.findViewById(R.id.tv_item_prescription_pill_division);
            pillFormulation = itemView.findViewById(R.id.tv_item_prescription_pill_formulation);
            classification = itemView.findViewById(R.id.tv_item_prescription_medicine_classification);
            efficacy1 = itemView.findViewById(R.id.tv_item_prescription_efficacy1);
            efficacy2 = itemView.findViewById(R.id.tv_item_prescription_efficacy2);
        }
    }
}
