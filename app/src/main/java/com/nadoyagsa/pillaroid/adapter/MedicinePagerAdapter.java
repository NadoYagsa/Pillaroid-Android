package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.data.MedicineInfo;

import java.util.HashMap;

public class MedicinePagerAdapter extends RecyclerView.Adapter<MedicinePagerAdapter.ResultViewHolder> {
    private Context context;
    private MedicineInfo medicineInfo;
    public final HashMap<Integer,String> categories;

    public MedicinePagerAdapter(MedicineInfo medicineInfo) {
        this.medicineInfo = medicineInfo;
        categories = new HashMap<Integer,String>(){
            {
                put(0, "효능 및 효과");
                put(1, "용법 및 용량");
                put(2, "주의사항");
                put(3, "외형");
                put(4, "성분");
                put(5, "보관법");
            }
        };
    }

    public void setMedicineInfo(MedicineInfo medicineInfo) {
        this.medicineInfo = medicineInfo;
    }

    public HashMap<Integer, String> getCategories() {
        return categories;
    }

    @NonNull
    @Override
    public MedicinePagerAdapter.ResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_medicine_result, parent, false);
        MedicinePagerAdapter.ResultViewHolder viewHolder = new ResultViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MedicinePagerAdapter.ResultViewHolder holder, int position) {
        holder.tvCategory.setText(categories.get(position));

        if (position == 3 && medicineInfo.getAppearanceInfo() != null) {    // 외형 출력
            holder.svContent.setVisibility(View.GONE);
            holder.svAppearanceContent.setVisibility(View.VISIBLE);

            // 외형 정보 설정
            if (medicineInfo.getAppearanceInfo().getAppearance() == null)   // 성상
                holder.llAppearance.setVisibility(View.GONE);
            else {
                holder.llAppearance.setVisibility(View.VISIBLE);
                holder.tvAppearance.setText(medicineInfo.getAppearanceInfo().getAppearance());
            }
            if (medicineInfo.getAppearanceInfo().getFormulation() == null)  // 제형
                holder.llFormulation.setVisibility(View.GONE);
            else {
                holder.llFormulation.setVisibility(View.VISIBLE);
                holder.tvFormulation.setText(medicineInfo.getAppearanceInfo().getFormulation());
            }
            if (medicineInfo.getAppearanceInfo().getShape() == null)        // 모양
                holder.llShape.setVisibility(View.GONE);
            else {
                holder.llShape.setVisibility(View.VISIBLE);
                holder.tvShape.setText(medicineInfo.getAppearanceInfo().getShape());
            }
            if (medicineInfo.getAppearanceInfo().getColor() == null)        // 색상
                holder.llColor.setVisibility(View.GONE);
            else {
                holder.llColor.setVisibility(View.VISIBLE);
                holder.tvColor.setText(medicineInfo.getAppearanceInfo().getColor());
            }
            if (medicineInfo.getAppearanceInfo().getDividingLine() == null) // 분할선
                holder.llDividingLine.setVisibility(View.GONE);
            else {
                holder.llDividingLine.setVisibility(View.VISIBLE);
                holder.tvDividingLine.setText(medicineInfo.getAppearanceInfo().getDividingLine());
            }
            if (medicineInfo.getAppearanceInfo().getIdentificationMark() == null)   // 식별 표기
                holder.llIdentificationMark.setVisibility(View.GONE);
            else {
                holder.llIdentificationMark.setVisibility(View.VISIBLE);
                holder.tvIdentificationMark.setText(medicineInfo.getAppearanceInfo().getIdentificationMark());
            }
        }
        else {
            holder.svContent.setVisibility(View.VISIBLE);
            holder.svAppearanceContent.setVisibility(View.GONE);

            switch (position) {
                case 0: {   // 효능 및 효과
                    if (medicineInfo.getEfficacy() != null)
                        holder.tvContent.setText(medicineInfo.getEfficacy());
                    break;
                }
                case 1: {   // 용법 및 용량
                    if (medicineInfo.getUsage() != null)
                        holder.tvContent.setText(medicineInfo.getUsage());
                    break;
                }
                case 2: {   // 주의사항
                    if (medicineInfo.getPrecautions() != null)
                        holder.tvContent.setText(medicineInfo.getPrecautions());
                    break;
                }
                case 4: {   // 성분
                    if (medicineInfo.getIngredient() != null)
                        holder.tvContent.setText(medicineInfo.getIngredient());
                    break;
                }
                case 5: {   // 보관법
                    if (medicineInfo.getSave() != null)
                        holder.tvContent.setText(medicineInfo.getSave());
                    break;
                }
            }
        }
    }

    @Override
    public int getItemCount() { return 6; }     // 효능 및 효과, 용법 및 용량, 주의사항, 외형, 성분, 보관법

    public static class ResultViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llAppearance, llFormulation, llShape, llColor, llDividingLine, llIdentificationMark;
        ScrollView svContent, svAppearanceContent;
        TextView tvCategory, tvContent;
        TextView tvAppearance, tvFormulation, tvShape, tvColor, tvDividingLine, tvIdentificationMark;

        ResultViewHolder(final View itemView) {
            super(itemView);

            tvCategory = itemView.findViewById(R.id.tv_item_medicineresult_category);
            // 외형 이외의 content 출력 뷰
            svContent = itemView.findViewById(R.id.sv_item_medicineresult_content);
            tvContent = itemView.findViewById(R.id.tv_item_medicineresult_content);

            // 외형의 content 출력 뷰
            svAppearanceContent = itemView.findViewById(R.id.sv_item_medicineresult_appearance);
            
            llAppearance = itemView.findViewById(R.id.ll_item_medicineresult_appearance);
            llFormulation = itemView.findViewById(R.id.ll_item_medicineresult_formulation);
            llShape = itemView.findViewById(R.id.ll_item_medicineresult_shape);
            llColor = itemView.findViewById(R.id.ll_item_medicineresult_color);
            llDividingLine = itemView.findViewById(R.id.ll_item_medicineresult_dividingline);
            llIdentificationMark = itemView.findViewById(R.id.ll_item_medicineresult_identification);
            
            tvAppearance = itemView.findViewById(R.id.tv_item_medicineresult_appearance);
            tvFormulation = itemView.findViewById(R.id.tv_item_medicineresult_formulation);
            tvShape = itemView.findViewById(R.id.tv_item_medicineresult_shape);
            tvColor = itemView.findViewById(R.id.tv_item_medicineresult_color);
            tvDividingLine = itemView.findViewById(R.id.tv_item_medicineresult_dividingline);
            tvIdentificationMark = itemView.findViewById(R.id.tv_item_medicineresult_identification);
        }
    }
}
