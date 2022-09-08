package com.nadoyagsa.pillaroid.adapter;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.nadoyagsa.pillaroid.LoginActivity;
import com.nadoyagsa.pillaroid.PillaroidAPIImplementation;
import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.SharedPrefManager;
import com.nadoyagsa.pillaroid.data.PrescriptionInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PrescriptionPagerAdapter extends RecyclerView.Adapter<PrescriptionPagerAdapter.ResultViewHolder> {
    private static final int IBT_FAVORITES = 1;
    private static final int DESCRIPTION_TEXT = 2;

    private Context context;
    private final ActivityResultLauncher<Intent> startActivityResultLogin;
    private final ArrayList<PrescriptionInfo> resultList;

    private long delay = 0;
    private Integer currentViewId = null;

    public PrescriptionPagerAdapter(ActivityResultLauncher<Intent> startActivityResultLogin, ArrayList<PrescriptionInfo> resultList) {
        this.startActivityResultLogin = startActivityResultLogin;
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
        holder.tvMedicineName.setText(resultList.get(position).getName());
        if (resultList.get(position).isFavoritesNull())
            holder.ibtFavorites.setImageResource(R.drawable.icon_star_off);
        else
            holder.ibtFavorites.setImageResource(R.drawable.icon_star_on);

        holder.ibtFavorites.setOnClickListener(view -> {
            if (System.currentTimeMillis() > delay) {
                currentViewId = IBT_FAVORITES;
                delay = System.currentTimeMillis() + 3000;
                tts.speak("버튼. 즐겨찾기", QUEUE_FLUSH, null, null);
            } else if (currentViewId == IBT_FAVORITES) {
                final int clickPosition = position;

                if (SharedPrefManager.read("token", null) == null) {
                    tts.speak("즐겨찾기 기능은 로그인이 필요합니다. 로그인을 하시려면 화면 하단의 카카오 로그인 버튼을 눌러주세요.", QUEUE_FLUSH, null, null);

                    Intent loginIntent = new Intent(context, LoginActivity.class);
                    loginIntent.putExtra("from", 'r');
                    startActivityResultLogin.launch(loginIntent);
                } else {  // 이미 로그인된 사용자
                    PrescriptionInfo prescription = resultList.get(clickPosition);

                    // TODO: 처음 즐겨찾기 추가 시에 선택된 의약품 명이 출력됨
                    if (prescription.isFavoritesNull()) {   // 즐겨찾기 추가
                        JsonObject request = new JsonObject();
                        request.addProperty("medicineIdx", prescription.getMedicineIdx());
                        PillaroidAPIImplementation.getApiService().postFavorites(SharedPrefManager.read("token", null), request).enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                if (response.code() == 201) {
                                    try {
                                        JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                                        JSONObject data = responseJson.getJSONObject("data");

                                        resultList.get(clickPosition).setFavoritesIdx(data.getLong("favoritesIdx"));
                                        tts.speak("즐겨찾기 추가", TextToSpeech.QUEUE_FLUSH, null, null);

                                        notifyItemChanged(clickPosition);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else if (response.code() == 401) {
                                    tts.speak("허가받지 않은 회원의 접근입니다.", QUEUE_FLUSH, null, null);
                                } else {
                                    tts.speak("즐겨찾기 추가에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                                tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
                            }
                        });
                    } else {                                              // 즐겨찾기 해제
                        PillaroidAPIImplementation.getApiService().deleteFavorites(SharedPrefManager.read("token", null), prescription.getFavoritesIdx()).enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                if (response.code() == 200) {
                                    resultList.get(clickPosition).setFavoritesIdx(null);
                                    tts.speak("즐겨찾기 삭제", TextToSpeech.QUEUE_FLUSH, null, null);

                                    notifyItemChanged(clickPosition);
                                } else if (response.code() == 401) {
                                    tts.speak("허가받지 않은 회원의 접근입니다.", QUEUE_FLUSH, null, null);
                                } else if (response.code() == 400) {
                                    if (response.errorBody() != null) {
                                        try {
                                            String errorStr = response.errorBody().string();
                                            JSONObject errorBody = new JSONObject(errorStr);
                                            long errorIdx = errorBody.getLong("errorIdx");

                                            if (errorIdx == 40001)  // 삭제 오류
                                                tts.speak("즐겨찾기에 추가되지 않은 의약품이기에 삭제가 불가합니다.", QUEUE_FLUSH, null, null);
                                        } catch (JSONException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else
                                        tts.speak("즐겨찾기 삭제에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                                } else {
                                    tts.speak("즐겨찾기 삭제에 문제가 생겼습니다.", QUEUE_FLUSH, null, null);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                tts.speak("서버와 연결이 되지 않습니다.", QUEUE_FLUSH, null, null);
                                tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
                            }
                        });
                    }
                }
            }
        });

        // 외형 정보
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

    public class ResultViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton ibtFavorites;
        LinearLayout llAppearance, llFeature, llFormulation, llShape, llColor, llDividingLine, llIdentificationMark, llDosage, llEfficacy;
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
            llDosage = itemView.findViewById(R.id.ll_item_prescription_disage);
            llEfficacy = itemView.findViewById(R.id.ll_item_prescription_efficacy);

            tvNoAppearance = itemView.findViewById(R.id.tv_item_prescription_noAppearance);
            tvFeature = itemView.findViewById(R.id.tv_item_prescription_feature);
            tvFormulation = itemView.findViewById(R.id.tv_item_prescription_formulation);
            tvShape = itemView.findViewById(R.id.tv_item_prescription_shape);
            tvColor = itemView.findViewById(R.id.tv_item_prescription_color);
            tvDividingLine = itemView.findViewById(R.id.tv_item_prescription_dividingline);
            tvIdentificationMark = itemView.findViewById(R.id.tv_item_prescription_identification);
            
            tvDosage = itemView.findViewById(R.id.tv_item_prescription_dosage);
            tvEfficacy = itemView.findViewById(R.id.tv_item_prescription_efficacy);

            llAppearance.setOnClickListener(v -> {
                currentViewId = DESCRIPTION_TEXT;
                tts.speak("텍스트. " + toAppearanceString(), QUEUE_FLUSH, null, null);
            });
            llDosage.setOnClickListener(llClickListener);
            llEfficacy.setOnClickListener(llClickListener);

            tvNoAppearance.setOnClickListener(v -> {
                currentViewId = DESCRIPTION_TEXT;
                tts.speak("텍스트. " + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
            });
        }

        View.OnClickListener llClickListener = v -> {
            currentViewId = DESCRIPTION_TEXT;
            TextView tvCategory = (TextView) ((LinearLayout) v).getChildAt(0);
            TextView tvContent = (TextView) ((LinearLayout) v).getChildAt(1);
            tts.speak("텍스트. " + tvCategory.getText() + ". " + tvContent.getText(), QUEUE_FLUSH, null, null);
        };

        private String toAppearanceString() {
            StringBuilder resultSb = new StringBuilder("외형 정보.");
            if (!tvFeature.getText().equals("")) {
                resultSb.append("성상. " + tvFeature.getText() + ". ");
            }
            if (!tvFormulation.getText().equals("")) {
                resultSb.append("제형. " + tvFormulation.getText() + ". ");
            }
            if (!tvShape.getText().equals("")) {
                resultSb.append("모양. " + tvShape.getText() + ". ");
            }
            if (!tvColor.getText().equals("")) {
                resultSb.append("색상. " + tvColor.getText() + ". ");
            }
            if (!tvDividingLine.getText().equals("")) {
                resultSb.append("분할선. " + tvDividingLine.getText() + ". ");
            }
            if (!tvIdentificationMark.getText().equals("")) {
                resultSb.append("식별표기. " + tvIdentificationMark.getText() + ". ");
            }
            return resultSb.toString();
        }
    }
}
