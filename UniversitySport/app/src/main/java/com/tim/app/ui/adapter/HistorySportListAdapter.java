package com.tim.app.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.application.library.log.DLOG;
import com.application.library.widget.recycle.BaseRecyclerAdapter;
import com.tim.app.R;
import com.tim.app.constant.AppConstant;
import com.tim.app.server.entry.HistoryAreaSportEntry;
import com.tim.app.server.entry.HistoryRunningSportEntry;
import com.tim.app.ui.activity.HistoryItem;
import com.tim.app.ui.activity.SportResultActivity;
import com.tim.app.ui.adapter.viewholder.ViewHolder;

import java.text.SimpleDateFormat;
import java.util.List;

import static com.lzy.okhttputils.OkHttpUtils.getContext;


public class HistorySportListAdapter extends BaseRecyclerAdapter<BaseRecyclerAdapter.BaseRecyclerViewHolder, HistoryItem>
        implements BaseRecyclerAdapter.OnItemClickListener {
    private Context mContext;
    private static final String TAG = "HistorySportListAdapter";
    private static Typeface typeface;

    public HistorySportListAdapter(Context context, List<HistoryItem> dataList) {
        super(dataList);
        this.mContext = context;
        this.setOnItemClickListener(this);
    }

    @Override
    public BaseRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseRecyclerViewHolder
                holder = new ViewHolder(mContext, LayoutInflater.from(parent.getContext()).inflate(R.layout.history_daily_record, null));
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseRecyclerViewHolder holder, final int position, HistoryItem data) {
        boolean isDelimit = false;
        if (data == null) {
            return;
        }

        int totalEnergyCost = 0;
        final ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.removeAllViews(R.id.llSportItem);

        for (int i = 0; i < data.historySportEntryList.size(); i++) {
            final LinearLayout ll = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.history_daily_record_item, null);

            //加上第一条分割线
            if (i == 0) {
                View vTopDelimiter = ll.findViewById(R.id.vTopDelimiter);
                vTopDelimiter.setVisibility(View.VISIBLE);
            }

            if (data.historySportEntryList.get(i).getType() == AppConstant.RUNNING_TYPE) {
                //向下转型
                final HistoryRunningSportEntry runningSportEntry = (HistoryRunningSportEntry) data.historySportEntryList.get(i);
                //                ll.setClickable(true);
                ll.setTag(runningSportEntry);

                //运动名字
                TextView tvSportDesc = (TextView) ll.findViewById(R.id.tvSportDesc);
                tvSportDesc.setText(runningSportEntry.getSportName());

                // 运动结果
                TextView tvResult = (TextView) ll.findViewById(R.id.tvResult);

                ImageView ivSportQualified = (ImageView) ll.findViewById(R.id.ivSportQualified);

                //                if (runningSportEntry.getEndedAt() == 0) {
                //                    ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_red));
                //                } else {
                //                    if (runningSportEntry.isValid()) {
                //                        if (runningSportEntry.isQualified()) {
                //                            ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_blue));
                //                        } else {
                //                            ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_orange));
                //                        }
                //                    } else {
                //                        ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_orange));
                //                    }
                //                }

                //非正常结束
                if (runningSportEntry.getEndedAt() == 0) {
                    tvResult.setText("未结束");
                    tvResult.setTextColor(getContext().getResources().getColor(R.color.orange_primary));
                    ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_orange));
                } else {
                    //是否达标
                    if (runningSportEntry.isQualified()) {
                        //是否审核
                        if (runningSportEntry.isVerified()) {
                            //是否有效
                            if (runningSportEntry.isValid()) {
                                tvResult.setText("达标");
                                tvResult.setTextColor(getContext().getResources().getColor(R.color.green_primary));
                                ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_blue));
                            } else {
                                tvResult.setText("审核未通过");
                                tvResult.setTextColor(getContext().getResources().getColor(R.color.red_primary));
                                ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_red));
                            }
                        } else {
                            tvResult.setText("达标待审核");
                            tvResult.setTextColor(getContext().getResources().getColor(R.color.green_primary));
                            ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_blue));
                        }
                    } else {
                        tvResult.setText("未达标");
                        tvResult.setTextColor(getContext().getResources().getColor(R.color.orange_primary));
                        ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_runman_orange));
                    }
                }

                TextView tvSportTime = (TextView) ll.findViewById(R.id.tvSportTime);
                //SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                tvSportTime.setText(sdf.format(runningSportEntry.getStartTime()));
                tvSportTime.setVisibility(View.VISIBLE);

                //距离/1000后单位为公里
                TextView tvLeft = (TextView) ll.findViewById(R.id.tvLeft);
                int distance = runningSportEntry.getDistance();
                // double distance = runningSportEntry.getDistance() * 1.0 / 1000;
                // String result = String.format("%.2f", distance);
                tvLeft.setText(distance + " ");
                tvLeft.setTypeface(getTypeface(mContext), Typeface.ITALIC);  //设置字体 斜体

                //设置单位
                TextView tvUnit = (TextView) ll.findViewById(R.id.tvUnit);
                tvUnit.setText("米");

                //耗时
                TextView tvMiddle = (TextView) ll.findViewById(R.id.tvMiddle);
                String time = com.tim.app.util.TimeUtil.formatMillisTime(runningSportEntry.getCostTime() * 1000);
                String hour = time.split(":")[0];
                if (Integer.parseInt(hour) < 10) {
                    time = "0" + time;
                }
                tvMiddle.setText(time);

                //消耗热量
                TextView tvRight = (TextView) ll.findViewById(R.id.tvRight);
                tvRight.setText(runningSportEntry.getKcalConsumed() + "");

                //隐藏
                TextView tvArea = (TextView) ll.findViewById(R.id.tvArea);
                ImageView ivArea = (ImageView) ll.findViewById(R.id.ivArea);
                tvArea.setVisibility(View.GONE);
                ivArea.setVisibility(View.GONE);

                //累加总共消耗热量
                totalEnergyCost += runningSportEntry.getKcalConsumed();

                ll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SportResultActivity.start(mContext, runningSportEntry);
                    }
                });


                viewHolder.addView(R.id.llSportItem, ll);
            } else {

                final HistoryAreaSportEntry areaSportEntry = (HistoryAreaSportEntry) data.historySportEntryList.get(i);

                //区域名字
                TextView tvSportDesc = (TextView) ll.findViewById(R.id.tvSportDesc);
                tvSportDesc.setText(areaSportEntry.getAreaSport());

                // 运动结果
                TextView tvResult = (TextView) ll.findViewById(R.id.tvResult);

                ImageView ivSportQualified = (ImageView) ll.findViewById(R.id.ivSportQualified);
                //                if (areaSportEntry.getEndedAt() == 0) {
                //                    ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_red));
                //                } else {
                //                    if (data.historySportEntryList.get(i).isQualified()) {
                //                        ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_blue));
                //                    } else {
                //                        ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_orange));
                //                    }
                //                }

                //非正常结束
                if (areaSportEntry.getEndedAt() == 0) {
                    tvResult.setText("未结束");
                    tvResult.setTextColor(getContext().getResources().getColor(R.color.orange_primary));
                    ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_orange));
                } else {
                    //是否达标
                    if (areaSportEntry.isQualified()) {
                        //是否审核
                        if (areaSportEntry.isVerified()) {
                            //是否有效
                            if (areaSportEntry.isValid()) {
                                tvResult.setText("达标");
                                tvResult.setTextColor(getContext().getResources().getColor(R.color.green_primary));
                                ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_blue));
                            } else {
                                tvResult.setText("审核未通过");
                                tvResult.setTextColor(getContext().getResources().getColor(R.color.red_primary));
                                ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_red));
                            }
                        } else {
                            tvResult.setText("达标待审核");
                            tvResult.setTextColor(getContext().getResources().getColor(R.color.green_primary));
                            ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_blue));
                        }
                    } else {
                        //未达标
                        tvResult.setText("未达标");
                        tvResult.setTextColor(getContext().getResources().getColor(R.color.orange_primary));
                        ivSportQualified.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_area_orange));
                    }
                }

                TextView tvSportTime = (TextView) ll.findViewById(R.id.tvSportTime);
                //SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                tvSportTime.setText(sdf.format(areaSportEntry.getStartTime()));
                tvSportTime.setVisibility(View.VISIBLE);

                //耗时
                TextView tvLeft = (TextView) ll.findViewById(R.id.tvLeft);
                Integer costTime = areaSportEntry.getCostTime() / 60;
                String result = String.valueOf(costTime) + " ";
                tvLeft.setText(result);
                tvLeft.setTypeface(getTypeface(getContext()), Typeface.ITALIC);  //设置字体 斜体

                //设置单位
                TextView tvUnit = (TextView) ll.findViewById(R.id.tvUnit);
                tvUnit.setText("分钟");

                //耗时
                TextView tvMiddle = (TextView) ll.findViewById(R.id.tvMiddle);
                ImageView ivMiddle = (ImageView) ll.findViewById(R.id.ivMiddle);
                tvMiddle.setVisibility(View.GONE);
                ivMiddle.setVisibility(View.GONE);
                //                String time = com.tim.app.util.TimeUtil.formatMillisTime(areaSportEntry.getCostTime() * 1000);
                //                String hour = time.split(":")[0];
                //                if(Integer.parseInt(hour)<10){
                //                    time = "0" + time;
                //                }
                //                tvMiddle.setText(time);

                //消耗热量
                TextView tvRight = (TextView) ll.findViewById(R.id.tvRight);
                tvRight.setText(areaSportEntry.getKcalConsumed() + "");

                TextView tvArea = (TextView) ll.findViewById(R.id.tvArea);
                tvArea.setText(areaSportEntry.getLocationPoint().getAreaName());


                //累加总共消耗热量
                totalEnergyCost += areaSportEntry.getKcalConsumed();

                ll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SportResultActivity.start(mContext, areaSportEntry);
                    }
                });

                viewHolder.addView(R.id.llSportItem, ll);
            }
        }
        //当天日期
        viewHolder.setText(R.id.tvSportDate, data.date);
        //一天的热量消耗
        viewHolder.setText(R.id.tvEnergyCost, mContext.getString(R.string.kcalPlaceholder, String.valueOf(totalEnergyCost)));
    }

    public static Typeface getTypeface(Context context) {
        if (typeface == null) {
            typeface = Typeface.createFromAsset(context.getAssets(), "fonts/RussoOne-Regular.ttf");
        }
        return typeface;
    }

    @Override
    public void onItemClick(View view, int position, long id) {
        DLOG.d(TAG, "onItemClick");
        DLOG.d(TAG, "position:" + position);
        DLOG.d(TAG, "id:" + id);
        DLOG.d(TAG, "getDataList().get(position).historySportEntryList.get(0):" + getDataList().get(position).historySportEntryList.get(0));
        //        SportResultActivity.start(mContext, getDataList().get(position).historySportEntryList.get());
    }
}
