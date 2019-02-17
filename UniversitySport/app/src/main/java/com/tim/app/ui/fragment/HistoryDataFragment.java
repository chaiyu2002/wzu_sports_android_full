package com.tim.app.ui.fragment;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.widget.EmptyLayout;
import com.application.library.widget.loadmore.LoadMoreContainer;
import com.application.library.widget.loadmore.LoadMoreHandler;
import com.application.library.widget.loadmore.LoadMoreRecycleViewContainer;
import com.application.library.widget.recycle.WrapRecyclerView;
import com.gavin.com.library.StickyDecoration;
import com.gavin.com.library.listener.GroupListener;
import com.gavin.com.library.listener.OnGroupClickListener;
import com.lzy.okhttputils.OkHttpUtils;
import com.tim.app.R;
import com.tim.app.constant.AppConstant;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.FixLocationOutdoorSportPoint;
import com.tim.app.server.entry.HistoryAreaSportEntry;
import com.tim.app.server.entry.HistoryRunningSportEntry;
import com.tim.app.server.entry.HistorySportEntry;
import com.tim.app.ui.activity.HistoryItem;
import com.tim.app.ui.activity.HistorySportActivity;
import com.tim.app.ui.adapter.HistorySportListAdapter;
import com.tim.app.ui.view.HistoryDataHeadView;
import com.tim.app.util.DensityUtil;
import com.tim.app.util.MyDateUtil;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.tim.app.constant.AppConstant.student;


/**
 * 历史数据
 */
public class HistoryDataFragment extends LazyFragment implements View.OnClickListener, LoadMoreHandler, HistorySportActivity.PageSelectListener {

    public static final String TAG = "HistoryDataFragment";
    private static final int PAGE_SIZE = 20;
    int type;
    private View rootView;
    private LoadMoreRecycleViewContainer lrvLoadMore;
    private WrapRecyclerView wrvHistoryData;
    private EmptyLayout emptyLayout;
    private HistorySportListAdapter adapter;
    private List<HistoryItem> dataList = new ArrayList<HistoryItem>();
    private List<HistoryItem> dataListAll = new ArrayList<HistoryItem>();               //全部
    private List<HistoryItem> dataListQualified = new ArrayList<HistoryItem>();         //达标
    private List<HistoryItem> dataListDisqualified = new ArrayList<HistoryItem>();      //未达标
    private List<HistoryItem> dataListNotVerified = new ArrayList<HistoryItem>();       //达标待审核
    private List<HistoryItem> dataListVerifiedFailed = new ArrayList<HistoryItem>();      //审核未通过
    private List<HistoryItem> dataListAbnormalEnd = new ArrayList<HistoryItem>();       //未结束

    public int currentFragmentState = -1;

    private HistoryDataHeadView headView;
    private int universityId;
    //    private int studentId = 2;

    private int pageCountWeek;
    private int pageSizeWeek = 6;
    private int pageNoWeek = 1;

    private int pageCountMonth;
    private int pageSizeMonth = 6;
    private int pageNoMonth = 1;

    private int pageCountTerm;
    private int pageSizeTerm = 6;
    private int pageNoTerm = 1;

    private int pageCountHistory;
    private int pageSizeHistory = 6;
    private int pageNoHistory = 1;

    private String tabStartDate;
    private String tabEndDate;

    //    private String monthTabStartDate;
    //    private String monthTabEndDate;
    //
    //    private String termTabStartDate;
    //    private String termTabEndDate;
    //
    //    private String historyTabStartDate;
    //    private String historyTabEndDate;

    private String termStartDate = new LocalDate(MyDateUtil.getCurrentMonthStartDate()).toString();

    public static HistoryDataFragment newInstance(int type, int state) {
        HistoryDataFragment fragment = new HistoryDataFragment();
        Bundle args = new Bundle();
        args.putInt("type", type);
        args.putInt("state", state);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onCreateViewLazy(Bundle savedInstanceState) {
        super.onCreateViewLazy(savedInstanceState);
        setContentView(R.layout.fragment_history);
        if (null == rootView) {
            rootView = getContentView();

            lrvLoadMore = (LoadMoreRecycleViewContainer) rootView.findViewById(R.id.lrvLoadMore);
            wrvHistoryData = (WrapRecyclerView) rootView.findViewById(R.id.wrvHistoryData);
            //去除滑动到顶部或者是底部时会出现阴影的问题
            //            wrvHistoryData.setOverScrollMode(View.OVER_SCROLL_NEVER);

            lrvLoadMore.useDefaultFooter(View.GONE);
            lrvLoadMore.setAutoLoadMore(true);
            lrvLoadMore.setLoadMoreHandler(this);

            emptyLayout = new EmptyLayout(getActivity(), lrvLoadMore);
            emptyLayout.showLoading();
            emptyLayout.setEmptyButtonShow(false);
            emptyLayout.setErrorButtonShow(true);
            emptyLayout.setEmptyDrawable(R.drawable.ic_empty_hisorty_data);
            emptyLayout.setEmptyText("当前没有运动记录");
            emptyLayout.setEmptyButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    emptyLayout.showLoading();
                    initData();
                }
            });

            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            wrvHistoryData.setLayoutManager(layoutManager);
            //wrvHistoryData.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).color(getResources().getColor(R.color.colorLine)).size((int) getResources().getDimension(R.dimen.dimen_2)).build());
            //wrvHistoryData.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).color(getResources().getColor(R.color.colorLine2)).size((int) getResources().getDimension(R.dimen.dimen_1)).build());

            headView = (HistoryDataHeadView) LayoutInflater.from(getActivity()).inflate(R.layout.history_data_head_view, null);

            wrvHistoryData.addHeaderView(headView);

            adapter = new HistorySportListAdapter(getActivity(), dataList);
            wrvHistoryData.setAdapter(adapter);

            if (getArguments() != null) {
                type = getArguments().getInt("type");
                currentFragmentState = getArguments().getInt("state");
            }

        }
        headView.setData("0", "0", "0", "0", "0");
        initData();

        //回调
        GroupListener groupListener = new GroupListener() {
            @Override
            public String getGroupName(int position) {
                //根据position获取对应的组名称
                if (position == 0) {
                    return "";
                } else {
                    if (position <= dataList.size()) {
                        return dataList.get(position - 1).date;
                    } else {
                        return "";
                    }
                }
            }
        };
        //创建StickyDecoration，实现悬浮栏
        StickyDecoration decoration = StickyDecoration.Builder
                .init(groupListener)
                .setGroupBackground(Color.parseColor("#ffffff"))  //背景色（默认 透明）
                .setGroupHeight(DensityUtil.dp2px(this.getContext(), 32))     //高度
                .setDivideColor(Color.parseColor("#CCCCCC"))      //分割线颜色（默认 #CCCCCC）
                .setDivideHeight(DensityUtil.dp2px(this.getContext(), 1))     //分割线高宽度 (默认 0)
                .setGroupTextColor(Color.parseColor("#7A8084"))                 //字体颜色 （默认 Color.WHITE）
                .setGroupTextSize(DensityUtil.sp2px(this.getContext(), 20))    //字体大小 （默认 40）
                .setTextSideMargin(DensityUtil.dp2px(this.getContext(), 20))  //边距   靠左时为左边距  靠右时为右边距（默认 10）
                .isAlignLeft(true)                               //靠右显示  （默认 靠左）
                .setOnClickListener(new OnGroupClickListener() {  //点击事件，返回当前分组下第一个item的position
                    @Override
                    public void onClick(int position) {
                        //处理点击事件
                    }
                })
                .build();

        wrvHistoryData.addItemDecoration(decoration);


        setHasOptionsMenu(true);

    }


    private void getHistoryRecord() {
        ServerInterface.instance().queryHistorySportsRecord(student.getId(), tabStartDate, tabEndDate, type, new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                Date startDate = null;
                Date endDate = null;
                // 点击刷新，清空原来的内容（解决点击刷新之后出现重复信息）
                if (dataList != null) {
                    dataList.clear();
                }
                if (errCode == 0) {

                    JSONObject student = json.optJSONObject("data").optJSONObject("student");
                    try {
                        int accuRunningActivityCount = student.optInt("accuRunningActivityCount");
                        int accuAreaActivityCount = student.optInt("accuAreaActivityCount");

                        //                        int qualifiedRunningActivityCount = student.optInt("qualifiedRunningActivityCount");
                        //                        int qualifiedAreaActivityCount = student.optInt("qualifiedAreaActivityCount");

                        String totalSignInCount = String.valueOf(student.optInt("signInCount"));

                        int runningActivityTimeCosted = student.optInt("runningActivityTimeCosted");
                        int areaActivityTimeCosted = student.optInt("areaActivityTimeCosted");

                        int runningActivityKcalConsumption = student.optInt("runningActivityKcalConsumption");
                        int areaActivityKcalConsumption = student.optInt("areaActivityKcalConsumption");

                        String totalActivityCount = String.valueOf(accuAreaActivityCount + accuRunningActivityCount);
                        //                        String totalqualifiedActivityCount = String.valueOf(qualifiedAreaActivityCount + qualifiedRunningActivityCount);
                        String totalActivityTimeCosted = String.valueOf(runningActivityTimeCosted + areaActivityTimeCosted);
                        String toalActivityKcalConsumption = String.valueOf(runningActivityKcalConsumption + areaActivityKcalConsumption);

                        if (type == AppConstant.THIS_WEEK) {
                            headView.setData("本周累计运动(次)", totalActivityCount, totalSignInCount, toalActivityKcalConsumption, totalActivityTimeCosted);
                        } else if (type == AppConstant.THIS_MONTH) {
                            headView.setData("本月累计运动(次)", totalActivityCount, totalSignInCount, toalActivityKcalConsumption, totalActivityTimeCosted);
                        } else {
                            headView.setData("本学期累计运动(次)", totalActivityCount, totalSignInCount, toalActivityKcalConsumption, totalActivityTimeCosted);
                        }

                        JSONArray runningSportArray = student.optJSONObject("runningActivities").optJSONArray("data");
                        JSONArray areaSportArray = student.optJSONObject("areaActivities").optJSONArray("data");

                        for (LocalDate date = new LocalDate(tabEndDate); date.isAfter(new LocalDate(tabStartDate).minusDays(1)); date = date.minusDays(1)) {
                            //                            Log.d(TAG, "date: " + date);
                            HistoryItem item = new HistoryItem();
                            item.historySportEntryList = null;
                            for (int i = 0; i < runningSportArray.length(); i++) {
                                if ((runningSportArray.optJSONObject(i).optString("sportDate")).equals(date.toString())) {
                                    HistoryRunningSportEntry entry = new HistoryRunningSportEntry();
                                    entry.setId(runningSportArray.optJSONObject(i).optInt("id"));
                                    entry.setSportId(runningSportArray.optJSONObject(i).optInt("runningSportId"));
                                    entry.setCostTime(Integer.valueOf(runningSportArray.optJSONObject(i).optString("costTime")));
                                    entry.setDistance(Integer.valueOf(runningSportArray.optJSONObject(i).optString("distance")));
                                    entry.setKcalConsumed(Integer.valueOf(runningSportArray.optJSONObject(i).optString("kcalConsumed")));
                                    entry.setQualified(runningSportArray.optJSONObject(i).optBoolean("qualified"));
                                    entry.setStartTime(Long.valueOf(runningSportArray.optJSONObject(i).optString("startTime")));
                                    entry.setSportDate(String.valueOf(runningSportArray.optJSONObject(i).optString("sportDate")));
                                    //过滤不完整数据
                                    //                                    if (Long.valueOf(runningSportArray.optJSONObject(i).getString("endedAt")) > 0) {
                                    entry.setEndedAt(Long.valueOf(runningSportArray.optJSONObject(i).getString("endedAt")));
                                    entry.setValid(runningSportArray.optJSONObject(i).getBoolean("isValid"));
                                    entry.setVerified(runningSportArray.optJSONObject(i).getBoolean("isVerified"));
                                    //                                    } else {
                                    //                                        continue;
                                    //                                    }
                                    entry.setSportName(runningSportArray.optJSONObject(i).optJSONObject("runningSport").optString("name"));
                                    entry.setType(AppConstant.RUNNING_TYPE);

                                    if (item.historySportEntryList == null) {
                                        item.historySportEntryList = new ArrayList<HistorySportEntry>();
                                    }
                                    item.historySportEntryList.add(entry);
                                }
                            }

                            for (int i = 0; i < areaSportArray.length(); i++) {
                                if ((areaSportArray.optJSONObject(i).optString("sportDate")).equals(date.toString())) {
                                    HistoryAreaSportEntry entry = new HistoryAreaSportEntry();
                                    entry.setId(areaSportArray.optJSONObject(i).optInt("id"));
                                    entry.setSportId(areaSportArray.optJSONObject(i).optInt("areaSportId"));

                                    entry.setCostTime(Integer.valueOf(areaSportArray.optJSONObject(i).optString("costTime")));
                                    entry.setKcalConsumed(Integer.valueOf(areaSportArray.optJSONObject(i).optString("kcalConsumed")));
                                    entry.setQualified(areaSportArray.optJSONObject(i).optBoolean("qualified"));
                                    entry.setValid(areaSportArray.optJSONObject(i).optBoolean("isValid"));
                                    entry.setVerified(areaSportArray.optJSONObject(i).optBoolean("isVerified"));
                                    entry.setStartTime(Long.valueOf(areaSportArray.optJSONObject(i).optString("startTime")));
                                    entry.setSportDate(String.valueOf(areaSportArray.optJSONObject(i).optString("sportDate")));
                                    entry.setEndedAt(Long.valueOf(areaSportArray.optJSONObject(i).getString("endedAt")));

                                    JSONObject point = areaSportArray.getJSONObject(i).getJSONObject("location");
                                    FixLocationOutdoorSportPoint locationPoint = new FixLocationOutdoorSportPoint();
                                    locationPoint.setAreaName(point.getString("name"));
                                    locationPoint.setAddress(point.getString("addr"));
                                    locationPoint.setEnabled(point.getBoolean("isEnabled"));
                                    locationPoint.setLatitude(Double.parseDouble(point.getString("latitude")));
                                    locationPoint.setLongitude(Double.parseDouble(point.getString("longitude")));
                                    locationPoint.setRadius(point.getInt("radius"));
                                    entry.setAreaSport(areaSportArray.optJSONObject(i).getJSONObject("areaSport").getString("name"));
                                    entry.setLocationPoint(locationPoint);
                                    entry.setType(AppConstant.AREA_TYPE);

                                    if (item.historySportEntryList == null) {
                                        item.historySportEntryList = new ArrayList<HistorySportEntry>();
                                    }
                                    item.historySportEntryList.add(entry);
                                }
                            }

                            if (item.historySportEntryList != null) {
                                //                                for (int i = 0; i < item.historySportEntryList.size(); i++) {
                                //                                    Log.d(TAG, item.historySportEntryList.get(i).toString());
                                //                                }
                                // 对item.historySportEntryList进行排序
                                //                                TimeComparator timeComparator = new TimeComparator();
                                //                                Collections.sort(item.historySportEntryList, timeComparator);

                                item.date = date.toString();
                                dataList.add(item);
                            }
                        }

                        sortSportData();

                        // 显示当前需要筛选的内容
                        if (currentFragmentState != -1) {
                            if (((HistorySportActivity) getActivity()).currentState != -1) {
                                currentFragmentState = ((HistorySportActivity) getActivity()).currentState;  // 如果有前一次筛选记录
                            } else {
                                // TODO 第一次初始化，默认是显示 "达标" 记录。
                            }

                            changeHistoryDataList(null, currentFragmentState);
                        }

                        adapter.notifyDataSetChanged();
                        if (dataList.size() == 0) {
                            emptyLayout.showEmpty();
                        } else {
                            emptyLayout.showContent();
                        }
                        return true;
                    } catch (org.json.JSONException e) {
                        emptyLayout.showEmptyOrError(errCode);
                        e.printStackTrace();
                        DLOG.e(TAG, "queryHistorySportsRecord onJsonResponse e: " + e);
                        return false;
                    }
                } else {
                    emptyLayout.showEmptyOrError(errCode);
                    return false;
                }
            }

        });
    }

    private void initData() {
        tabEndDate = new LocalDate(new Date()).toString();
        if (type == AppConstant.THIS_WEEK) {
            tabStartDate = new LocalDate(MyDateUtil.getCurrentWeekStartDate()).toString();
            getHistoryRecord();
        } else if (type == AppConstant.THIS_MONTH) {
            tabStartDate = new LocalDate(MyDateUtil.getCurrentMonthStartDate()).toString();
            getHistoryRecord();
        } else if ((type == AppConstant.THIS_TERM)) {
            ServerInterface.instance().queryTermInfo(0, new JsonResponseCallback() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    try {
                        Date dt = new Date(json.optJSONObject("data").optJSONObject("term").getLong("startDate"));
                        termStartDate = sdf.format(dt);
                        tabStartDate = termStartDate;
                        getHistoryRecord();
                    } catch (Exception e) {
                        e.printStackTrace();
                        getHistoryRecord();
                        return false;
                    }
                    return true;
                }
            });
        } else {
            tabStartDate = new LocalDate(MyDateUtil.getCurrentMonthStartDate()).toString();
            getHistoryRecord();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    //
    // @Override
    // public void onStop() {
    //     super.onStop();
    // }
    //
    // @Override
    // public void onResume() {
    //     super.onResume();
    // }
    //
    // @Override
    // public void onPause() {
    //     super.onPause();
    // }
    //
    // @Override
    // public void onDestroyView() {
    //     super.onDestroyView();
    //     if (null != rootView) {
    //         ((ViewGroup) rootView.getParent()).removeView(rootView);
    //     }
    // }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OkHttpUtils.getInstance().cancelTag(TAG);
    }

    @Override
    public void onClick(View v) {
        DLOG.d(TAG, "onClick");
        switch (v.getId()) {
            default:
                break;
        }
    }

    @Override
    public void onLoadMore(LoadMoreContainer loadMoreContainer) {
        if (type == AppConstant.THIS_WEEK) {
            lrvLoadMore.loadMoreFinish(false, false);
        } else if (type == AppConstant.THIS_MONTH) {
            lrvLoadMore.loadMoreFinish(false, false);
        } else if (type == AppConstant.THIS_TERM) {
            LocalDate ld = new LocalDate(tabStartDate);
            if (ld.isBefore(new LocalDate(termStartDate))) {
                ld = ld.minusDays(1);
                tabEndDate = new LocalDate(tabStartDate).minusDays(1).toString();
                tabStartDate = ld.toString();
                getHistoryRecord();
                lrvLoadMore.loadMoreFinish(false, true);
            } else {
                lrvLoadMore.loadMoreFinish(false, false);
            }
        }
        //        } else if (type == AppConstant.THIS_MONTH) {
        //            ServerInterface.instance().queryHistorySportsRecord(studentId, pageNoMonth, pageSizeMonth, type, new JsonResponseCallback() {
        //                @Override
        //                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
        //                    if (errCode == 0) {
        //                        try {
        //                            JSONArray historySportArray = json.optJSONObject("data").optJSONObject("student").optJSONObject("currentMonthActivities").
        //                                    getJSONArray("data");
        //                            for (int i = 0; i < historySportArray.length(); i++) {
        //                                HistoryRunningSportEntry data = new HistoryRunningSportEntry();
        //                                data.setRunningSportId(historySportArray.getJSONObject(i).getInt("id"));
        //                                data.setName(historySportArray.getJSONObject(i).optJSONObject("runningSport").getString("name"));
        //                                data.setStartTime(Long.valueOf(historySportArray.getJSONObject(i).getString("startTime")));
        //                                data.setKcalConsumed(Integer.valueOf(historySportArray.getJSONObject(i).getString("kcalConsumed")));
        //                                data.setCostTime(Integer.valueOf(historySportArray.getJSONObject(i).getString("costTime")));
        //                                data.setSportDistance(Integer.valueOf(historySportArray.getJSONObject(i).getString("distance")));
        //                                data.setQualified(historySportArray.getJSONObject(i).getBoolean("qualified"));
        //                                dataList.add(data);
        //                            }
        //                            adapter.notifyDataSetChanged();
        //                            return true;
        //                        } catch (org.json.JSONException e) {
        //                            e.printStackTrace();
        //                            Log.e(TAG, "queryHistorySportsRecord onJsonResponse e: " + e);
        //                            return false;
        //                        }
        //                    } else {
        //                        return false;
        //                    }
        //                }
        //
        //            });
        //
        //            if (pageNoMonth != pageCountMonth) {
        //                pageNoMonth++;
        //                lrvLoadMore.loadMoreFinish(false, true);
        //            } else {
        //                lrvLoadMore.loadMoreFinish(false, false);
        //            }
        //
        //        } else if (type == AppConstant.THIS_TERM) {
        //            ServerInterface.instance().queryHistorySportsRecord(studentId, pageNoTerm, pageSizeTerm, type, new JsonResponseCallback() {
        //                @Override
        //                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
        //                    if (errCode == 0) {
        //                        try {
        //                            pageCountTerm = Integer.valueOf(json.optJSONObject("data").optJSONObject("student").optJSONObject("currentTermActivities").
        //                                    getString("pagesCount"));
        //                            JSONArray historySportArray = json.optJSONObject("data").optJSONObject("student").optJSONObject("currentTermActivities").
        //                                    getJSONArray("data");
        //                            for (int i = 0; i < historySportArray.length(); i++) {
        //                                HistoryRunningSportEntry data = new HistoryRunningSportEntry();
        //                                data.setRunningSportId(historySportArray.getJSONObject(i).getInt("id"));
        //                                data.setName(historySportArray.getJSONObject(i).optJSONObject("runningSport").getString("name"));
        //                                data.setStartTime(Long.valueOf(historySportArray.getJSONObject(i).getString("startTime")));
        //                                data.setKcalConsumed(Integer.valueOf(historySportArray.getJSONObject(i).getString("kcalConsumed")));
        //                                data.setCostTime(Integer.valueOf(historySportArray.getJSONObject(i).getString("costTime")));
        //                                data.setSportDistance(Integer.valueOf(historySportArray.getJSONObject(i).getString("distance")));
        //                                data.setQualified(historySportArray.getJSONObject(i).getBoolean("qualified"));
        //                                dataList.add(data);
        //                            }
        //                            adapter.notifyDataSetChanged();
        //                            return true;
        //                        } catch (org.json.JSONException e) {
        //                            e.printStackTrace();
        //                            Log.e(TAG, "queryHistorySportsRecord onJsonResponse e: " + e);
        //                            return false;
        //                        }
        //                    } else {
        //                        return false;
        //                    }
        //                }
        //
        //            });
        //
        //            if (pageNoTerm != pageCountTerm) {
        //                pageNoTerm++;
        //                lrvLoadMore.loadMoreFinish(false, true);
        //            } else {
        //                lrvLoadMore.loadMoreFinish(false, false);
        //            }
        //
        //        } else {
        //            ServerInterface.instance().queryHistorySportsRecord(studentId, pageNoHistory, pageSizeTerm, type, new JsonResponseCallback() {
        //                @Override
        //                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
        //                    if (errCode == 0) {
        //                        try {
        //                            pageCountTerm = Integer.valueOf(json.optJSONObject("data").optJSONObject("student").optJSONObject("activities").
        //                                    getString("pagesCount"));
        //                            JSONArray historySportArray = json.optJSONObject("data").optJSONObject("student").optJSONObject("activities").
        //                                    getJSONArray("data");
        //                            for (int i = 0; i < historySportArray.length(); i++) {
        //                                HistoryRunningSportEntry data = new HistoryRunningSportEntry();
        //                                data.setRunningSportId(historySportArray.getJSONObject(i).getInt("id"));
        //                                data.setName(historySportArray.getJSONObject(i).optJSONObject("runningSport").getString("name"));
        //                                data.setStartTime(Long.valueOf(historySportArray.getJSONObject(i).getString("startTime")));
        //                                data.setKcalConsumed(Integer.valueOf(historySportArray.getJSONObject(i).getString("kcalConsumed")));
        //                                data.setCostTime(Integer.valueOf(historySportArray.getJSONObject(i).getString("costTime")));
        //                                data.setSportDistance(Integer.valueOf(historySportArray.getJSONObject(i).getString("distance")));
        //                                dataList.add(data);
        //                            }
        //                            adapter.notifyDataSetChanged();
        //                            return true;
        //                        } catch (org.json.JSONException e) {
        //                            e.printStackTrace();
        //                            Log.e(TAG, "queryHistorySportsRecord onJsonResponse e: " + e);
        //                            return false;
        //                        }
        //                    } else {
        //                        return false;
        //                    }
        //                }
        //
        //            });
        //
        //            if (pageNoHistory != pageCountHistory) {
        //                pageNoHistory++;
        //                lrvLoadMore.loadMoreFinish(false, true);
        //            } else {
        //                lrvLoadMore.loadMoreFinish(false, false);
        //            }
        //        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPageSelected(int status) {

    }

    class TimeComparator implements Comparator<HistorySportEntry> {
        @Override
        public int compare(HistorySportEntry lhs, HistorySportEntry rhs) {
            Date ldate = new Date(lhs.getStartTime());
            //            Date ldate = new Date(lhs.getSportDate());
            Date rdate = new Date(rhs.getStartTime());
            //            Date rdate = new Date(rhs.getSportDate());
            return rdate.compareTo(ldate);
        }
    }

    /**
     * 根据选中的菜单设置当前显示的记录
     *
     * @param item 选中的菜单
     * @param type 当前fragment显示需要筛选的内容的类型
     */
    public void changeHistoryDataList(MenuItem item, int type) {
        if (dataList != null)
            dataList.clear();
        if (item != null) {
            switch (item.getItemId()) {
                case R.id.action_all:
                    dataList.addAll(dataListAll);
                    currentFragmentState = AppConstant.STATUS_ALL;
                    break;
                case R.id.action_qualified:
                    dataList.addAll(dataListQualified);
                    currentFragmentState = AppConstant.STATUS_QUALIFIED;
                    break;
                case R.id.action_disqualified:
                    dataList.addAll(dataListDisqualified);
                    currentFragmentState = AppConstant.STATUS_DISQUALIFIED;
                    break;
//                case R.id.action_not_verified:
//                    dataList.addAll(dataListNotVerified);
//                    currentFragmentState = AppConstant.STATUS_NOT_VERIFIED;
//                    break;
                case R.id.action_verified_fail:
                    dataList.addAll(dataListVerifiedFailed);
                    currentFragmentState = AppConstant.STATUS_VERIFIED_FAILED;
                    break;
//                case R.id.action_abnormal_end:
//                    dataList.addAll(dataListAbnormalEnd);
//                    currentFragmentState = AppConstant.STATUS_ABNORMAL_END;
//                    break;
                default:
                    super.onOptionsItemSelected(item);
                    break;
            }
        }

        if (type != -1) {
            switch (type) {
                case AppConstant.STATUS_ALL:
                    dataList.addAll(dataListAll);
                    currentFragmentState = AppConstant.STATUS_ALL;
                    break;
                case AppConstant.STATUS_QUALIFIED:
                    dataList.addAll(dataListQualified);
                    currentFragmentState = AppConstant.STATUS_QUALIFIED;
                    break;
                case AppConstant.STATUS_DISQUALIFIED:
                    dataList.addAll(dataListDisqualified);
                    currentFragmentState = AppConstant.STATUS_DISQUALIFIED;
                    break;
//                case AppConstant.STATUS_NOT_VERIFIED:
//                    dataList.addAll(dataListNotVerified);
//                    currentFragmentState = AppConstant.STATUS_NOT_VERIFIED;
//                    break;
                case AppConstant.STATUS_VERIFIED_FAILED:
                    dataList.addAll(dataListVerifiedFailed);
                    currentFragmentState = AppConstant.STATUS_VERIFIED_FAILED;
                    break;
//                case AppConstant.STATUS_ABNORMAL_END:
//                    dataList.addAll(dataListAbnormalEnd);
//                    currentFragmentState = AppConstant.STATUS_ABNORMAL_END;
//                    break;
                default:
                    super.onOptionsItemSelected(item);
                    break;
            }
        }
        ((HistorySportActivity) getActivity()).currentState = currentFragmentState;
        if (dataList.size() > 0) {
            emptyLayout.showContent();
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * 对所有历史记录进行整理，事先将记录分好类别。
     * 1、全部 2、达标 3、未达标 4、达标未审核 5、审核未通过 6、未结束
     */
    private void sortSportData() {

        if (dataListAll.size() != 0) {
            dataListAll.clear();
        }
        if (dataListQualified.size() != 0) {
            dataListQualified.clear();
        }
        if (dataListDisqualified.size() != 0) {
            dataListDisqualified.clear();
        }
        if (dataListNotVerified.size() != 0) {
            dataListNotVerified.clear();
        }
        if (dataListVerifiedFailed.size() != 0) {
            dataListVerifiedFailed.clear();
        }
        if (dataListAbnormalEnd.size() != 0) {
            dataListAbnormalEnd.clear();
        }

        for (HistoryItem item : dataList) {
            dataListAll.add(item);
        }

        for (HistoryItem item : dataList) {
            List<HistorySportEntry> historySportEntry = new ArrayList<>();
            for (HistorySportEntry entry : item.historySportEntryList) {
                //达标
                if (entry.isQualified() && entry.isValid() && entry.isVerified() && entry.getEndedAt() != 0) {
                    historySportEntry.add(entry);
                }
                //达标待审核
                if (entry.isQualified() && !entry.isVerified() && entry.getEndedAt() != 0) {
                    historySportEntry.add(entry);
                }
            }

            if (historySportEntry.size() != 0) {
                HistoryItem historyItem = new HistoryItem();
                historyItem.date = item.date;
                historyItem.historySportEntryList = historySportEntry;
                dataListQualified.add(historyItem);//实际为达标+达标待审核
            }
        }

        for (HistoryItem item : dataList) {
            List<HistorySportEntry> historySportEntry = new ArrayList<>();
            for (HistorySportEntry entry : item.historySportEntryList) {
                // 不达标，排除非正常结束
                if (!entry.isQualified() && entry.getEndedAt() != 0) {
                    historySportEntry.add(entry);
                }
            }

            if (historySportEntry.size() != 0) {
                HistoryItem historyItem = new HistoryItem();
                historyItem.date = item.date;
                historyItem.historySportEntryList = historySportEntry;
                dataListDisqualified.add(historyItem);
            }
        }

        for (HistoryItem item : dataList) {
            List<HistorySportEntry> historySportEntry = new ArrayList<>();
            for (HistorySportEntry entry : item.historySportEntryList) {
                if (entry.isQualified() && !entry.isVerified() && entry.getEndedAt() != 0) {
                    historySportEntry.add(entry);
                }
            }

            if (historySportEntry.size() != 0) {
                HistoryItem historyItem = new HistoryItem();
                historyItem.date = item.date;
                historyItem.historySportEntryList = historySportEntry;
                dataListNotVerified.add(historyItem);
            }
        }

        for (HistoryItem item : dataList) {
            List<HistorySportEntry> historySportEntry = new ArrayList<>();
            for (HistorySportEntry entry : item.historySportEntryList) {
                if (entry.isQualified() && entry.isVerified() && !entry.isValid() && entry.getEndedAt() != 0) {
                    historySportEntry.add(entry);
                }
            }

            if (historySportEntry.size() != 0) {
                HistoryItem historyItem = new HistoryItem();
                historyItem.date = item.date;
                historyItem.historySportEntryList = historySportEntry;
                dataListVerifiedFailed.add(historyItem);
            }
        }

        for (HistoryItem item : dataList) {
            List<HistorySportEntry> historySportEntry = new ArrayList<>();
            for (HistorySportEntry entry : item.historySportEntryList) {
                if (entry.getEndedAt() == 0) {
                    historySportEntry.add(entry);
                }

            }

            if (historySportEntry.size() != 0) {
                HistoryItem historyItem = new HistoryItem();
                historyItem.date = item.date;
                historyItem.historySportEntryList = historySportEntry;
                dataListAbnormalEnd.add(historyItem);
            }
        }
    }
}