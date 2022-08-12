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
    private final ArrayList<PrescriptionInfo> resultList;

    public PrescriptionPagerAdapter(ArrayList<PrescriptionInfo> resultList) {
        this.resultList = resultList;
    }

    @NonNull
    @Override
    public PrescriptionPagerAdapter.ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_prescription_result, parent, false);
        PrescriptionPagerAdapter.ResultViewHolder viewHolder = new ResultViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PrescriptionPagerAdapter.ResultViewHolder holder, int position) {
        holder.tvMedicineName.setText(resultList.get(position).getName());
        if (resultList.get(position).isFavorites())
            holder.ibtFavorites.setImageResource(R.drawable.icon_star_on);
        else
            holder.ibtFavorites.setImageResource(R.drawable.icon_star_off);

        if (resultList.get(position).getAppearanceInfo().isNull()) {
            holder.tvNoAppearance.setVisibility(View.VISIBLE);
            holder.llAppearance.setVisibility(View.GONE);
        }
        else {
            holder.tvNoAppearance.setVisibility(View.GONE);
            holder.llAppearance.setVisibility(View.VISIBLE);

            // 외형 정보 설정
            if (resultList.get(position).getAppearanceInfo().getFeature() == null)              // 성상
                holder.llFeature.setVisibility(View.GONE);
            else {
                holder.llFeature.setVisibility(View.VISIBLE);
                holder.tvFeature.setText(resultList.get(position).getAppearanceInfo().getFeature());
            }
            if (resultList.get(position).getAppearanceInfo().getFormulation() == null)          // 제형
                holder.llFormulation.setVisibility(View.GONE);
            else {
                holder.llFormulation.setVisibility(View.VISIBLE);
                holder.tvFormulation.setText(resultList.get(position).getAppearanceInfo().getFormulation());
            }
            if (resultList.get(position).getAppearanceInfo().getShape() == null)                // 모양
                holder.llShape.setVisibility(View.GONE);
            else {
                holder.llShape.setVisibility(View.VISIBLE);
                holder.tvShape.setText(resultList.get(position).getAppearanceInfo().getShape());
            }
            if (resultList.get(position).getAppearanceInfo().getColor() == null)                // 색상
                holder.llColor.setVisibility(View.GONE);
            else {
                holder.llColor.setVisibility(View.VISIBLE);
                holder.tvColor.setText(resultList.get(position).getAppearanceInfo().getColor());
            }
            if (resultList.get(position).getAppearanceInfo().getDividingLine() == null)         // 분할선
                holder.llDividingLine.setVisibility(View.GONE);
            else {
                holder.llDividingLine.setVisibility(View.VISIBLE);
                holder.tvDividingLine.setText(resultList.get(position).getAppearanceInfo().getDividingLine());
            }
            if (resultList.get(position).getAppearanceInfo().getIdentificationMark() == null)   // 식별 표기
                holder.llIdentificationMark.setVisibility(View.GONE);
            else {
                holder.llIdentificationMark.setVisibility(View.VISIBLE);
                holder.tvIdentificationMark.setText(resultList.get(position).getAppearanceInfo().getIdentificationMark());
            }
        }

        holder.tvDosage.setText(resultList.get(position).getDosage().replace(" ", "\u00A0"));
        holder.tvEfficacy.setText(resultList.get(position).getEfficacy().replace(" ", "\u00A0"));
    }

    @Override
    public int getItemCount() { return resultList.size(); }

    public static class ResultViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton ibtFavorites;
        LinearLayout llAppearance, llFeature, llFormulation, llShape, llColor, llDividingLine, llIdentificationMark;
        TextView tvNoAppearance, tvFeature, tvFormulation, tvShape, tvColor, tvDividingLine, tvIdentificationMark;
        TextView tvMedicineName, tvDosage, tvEfficacy;

        ResultViewHolder(final View itemView) {
            super(itemView);

            tvMedicineName = itemView.findViewById(R.id.tv_item_prescription_medicine_name);
            ibtFavorites = itemView.findViewById(R.id.ibt_item_prescription_favorites);

            llAppearance = itemView.findViewById(R.id.ll_item_prescription_appearance);
            llFeature = itemView.findViewById(R.id.ll_item_prescription_feature);
            llFormulation = itemView.findViewById(R.id.ll_item_prescription_formulation);
            llShape = itemView.findViewById(R.id.ll_item_prescription_shape);
            llColor = itemView.findViewById(R.id.ll_item_prescription_color);
            llDividingLine = itemView.findViewById(R.id.ll_item_prescription_dividingline);
            llIdentificationMark = itemView.findViewById(R.id.ll_item_prescription_identification);

            tvNoAppearance = itemView.findViewById(R.id.tv_item_prescription_noAppearance);
            tvFeature = itemView.findViewById(R.id.tv_item_prescription_feature);
            tvFormulation = itemView.findViewById(R.id.tv_item_prescription_formulation);
            tvShape = itemView.findViewById(R.id.tv_item_prescription_shape);
            tvColor = itemView.findViewById(R.id.tv_item_prescription_color);
            tvDividingLine = itemView.findViewById(R.id.tv_item_prescription_dividingline);
            tvIdentificationMark = itemView.findViewById(R.id.tv_item_prescription_identification);
            
            tvDosage = itemView.findViewById(R.id.tv_item_prescription_dosage);
            tvEfficacy = itemView.findViewById(R.id.tv_item_prescription_efficacy);
        }
    }
}
