package com.tim.app.ui.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.widget.EmptyLayout;
import com.application.library.widget.loadmore.LoadMoreContainer;
import com.application.library.widget.loadmore.LoadMoreHandler;
import com.application.library.widget.loadmore.LoadMoreRecycleViewContainer;
import com.application.library.widget.recycle.WrapRecyclerView;
import com.lzy.okhttputils.OkHttpUtils;
import com.tim.app.R;
import com.tim.app.constant.AppConstant;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.RankingData;
import com.tim.app.ui.adapter.RankingDataAdapter;
import com.tim.app.ui.view.RankingDataHeadView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tim.app.constant.AppConstant.student;

/**
 * 排行榜数据
 */
public class RankingDataFragment extends BaseFragment implements View.OnClickListener, LoadMoreHandler {

    public static final String TAG = "RankingDataFragment";
    private static final int PAGE_SIZE = 20;

    private View rootView;
    private LoadMoreRecycleViewContainer lrvLoadMore;
    private WrapRecyclerView wrvHistoryData;
    private EmptyLayout emptyLayout;

    private RankingDataAdapter adapter;
    private List<RankingData> dataList;

    private RankingDataHeadView headView;

    int type;
    // private int universityId = 1;
    private int pageNoEnergy = 1;
    private int pageSizeEnergy = 10; // value don't  less than 3
    private int pageCountEnergy = -1;

    private int pageNoTime = 1;
    private int pageSizeTime = 10; // value don't  less than 3
    private int pageCountTime = -1;

    public static RankingDataFragment newInstance(int type) {
        RankingDataFragment fragment = new RankingDataFragment();
        Bundle args = new Bundle();
        args.putInt("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null == rootView) {
            rootView = inflater.inflate(R.layout.fragment_history, container, false);

            lrvLoadMore = (LoadMoreRecycleViewContainer) rootView.findViewById(R.id.lrvLoadMore);
            wrvHistoryData = (WrapRecyclerView) rootView.findViewById(R.id.wrvHistoryData);
            wrvHistoryData.setOverScrollMode(View.OVER_SCROLL_NEVER);

            lrvLoadMore.useDefaultFooter(View.GONE);
            lrvLoadMore.setAutoLoadMore(true);
            lrvLoadMore.setLoadMoreHandler(this);

            emptyLayout = new EmptyLayout(getActivity(), lrvLoadMore);
            emptyLayout.showLoading();
            emptyLayout.setEmptyButtonShow(false);
            emptyLayout.setErrorButtonShow(true);
            emptyLayout.setEmptyDrawable(R.drawable.ic_empty_rank_data);
            emptyLayout.setEmptyText("当前没有排行榜数据");
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

            headView = (RankingDataHeadView) LayoutInflater.from(getActivity()).inflate(R.layout.rankingdata_head_view, null);
            wrvHistoryData.addHeaderView(headView);

            if (getArguments() != null) {
                type = getArguments().getInt("type");
            }

            dataList = new ArrayList<RankingData>();
            adapter = new RankingDataAdapter(getActivity(), dataList, type);
            wrvHistoryData.setAdapter(adapter);

        }
        initData();
        return rootView;
    }

    private void initData() {
        if (type == AppConstant.TYPE_COST_ENERGY) {
            ServerInterface.instance().queryCollegeSportsRankingData(student.getUniversityId(), pageSizeEnergy, pageNoEnergy, type, new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        try {
                            JSONObject jsonObject = json.getJSONObject("data").getJSONObject("university")
                                    .getJSONObject("kcalConsumptionRanking");

                            pageCountEnergy = Integer.valueOf(jsonObject.optString("pagesCount"));
                            JSONArray rankingDataArray = jsonObject.optJSONArray("data");
                            List<RankingData> headDataList = new ArrayList<>();
                            // if rankingDataArray#size greater than 3
                            for (int i = 0; i < rankingDataArray.length() && i != 3; i++) {
                                RankingData rankingData = new RankingData();
                                rankingData.setAvatar(rankingDataArray.getJSONObject(i).getString("avatarUrl"));
                                rankingData.setUserName(rankingDataArray.getJSONObject(i).getString("studentName"));
                                rankingData.setCostValue(Integer.valueOf(rankingDataArray.getJSONObject(i).getString("kcalConsumption")));
                                headDataList.add(rankingData);
                                DLOG.d(TAG, "rankingData:" + rankingData);
                            }
                            headView.setData(headDataList, AppConstant.TYPE_COST_ENERGY);

                            if (headDataList.size() > 0) {
                                emptyLayout.showContent();
                            } else {
                                emptyLayout.showEmpty();
                            }

                            // TODO
                            // headDataList.size()
                            for (int i = 3; i < rankingDataArray.length(); i++) {
                                RankingData data = new RankingData();
                                data.setAvatar("");
                                data.setUserName(rankingDataArray.getJSONObject(i).getString("studentName"));
                                data.setCostValue(Integer.valueOf(rankingDataArray.getJSONObject(i).getString("kcalConsumption")));
                                data.setAvatar(rankingDataArray.getJSONObject(i).getString("avatarUrl"));
                                dataList.add(data);
                                DLOG.d(TAG, "dataList:" + data);
                            }
                            adapter.notifyDataSetChanged();

                            if (pageNoEnergy != pageCountEnergy) {
                                lrvLoadMore.loadMoreFinish(false, true);
                            } else {
                                lrvLoadMore.loadMoreFinish(false, false);
                            }
                            pageNoEnergy++;

                            return true;
                        } catch (org.json.JSONException e) {
                            emptyLayout.showEmpty();
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        emptyLayout.showEmptyOrError(errCode);
                        return false;
                    }
                }

            });
        } else {
            ServerInterface.instance().queryCollegeSportsRankingData(student.getUniversityId(), pageSizeTime, pageNoTime, type, new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        try {
                            JSONObject jsonObject = json.getJSONObject("data").getJSONObject("university")
                                    .getJSONObject("timeCostedRanking");

                            pageCountTime = Integer.valueOf(jsonObject.getString("pagesCount"));
                            JSONArray rankingDataArray = jsonObject.getJSONArray("data");
                            List<RankingData> headDataList = new ArrayList<>();
                            // if rankingDataArray#size greater than 3
                            for (int i = 0; i < rankingDataArray.length() && i != 3; i++) {
                                RankingData rankingData = new RankingData();
                                rankingData.setAvatar(rankingDataArray.getJSONObject(i).getString("avatarUrl"));
                                rankingData.setUserName(rankingDataArray.getJSONObject(i).getString("studentName"));
                                rankingData.setCostValue(Integer.valueOf(rankingDataArray.getJSONObject(i).getString("timeCosted")));
                                headDataList.add(rankingData);
                                DLOG.d(TAG, "rankingData:" + rankingData);
                            }
                            headView.setData(headDataList, AppConstant.TYPE_COST_TIME);

                            if (headDataList.size() > 0) {
                                emptyLayout.showContent();
                            } else {
                                emptyLayout.showEmpty();
                            }

                            for (int i = 3; i < rankingDataArray.length(); i++) {
                                RankingData data = new RankingData();
                                data.setAvatar("");
                                data.setUserName(rankingDataArray.getJSONObject(i).getString("studentName"));
                                data.setCostValue(Integer.valueOf(rankingDataArray.getJSONObject(i).getString("timeCosted")));
                                data.setAvatar(rankingDataArray.getJSONObject(i).getString("avatarUrl"));
                                dataList.add(data);
                            }
                            adapter.notifyDataSetChanged();

                            if (pageNoTime != pageCountTime) {
                                lrvLoadMore.loadMoreFinish(false, true);
                            } else {
                                lrvLoadMore.loadMoreFinish(false, false);
                            }
                            pageNoTime++;

                            return true;
                        } catch (org.json.JSONException e) {
                            emptyLayout.showEmpty();
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        emptyLayout.showEmptyOrError(errCode);
                        return false;
                    }
                }

            });
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != rootView) {
            ((ViewGroup) rootView.getParent()).removeView(rootView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OkHttpUtils.getInstance().cancelTag(TAG);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
        }
    }

    @Override
    public void onLoadMore(LoadMoreContainer loadMoreContainer) {
        DLOG.d(TAG, "pageNoEnergy:" + pageNoEnergy);
        DLOG.d(TAG, "pageNoTime:" + pageNoTime);
        DLOG.d(TAG, "pageCountEnergy:" + pageCountEnergy);
        if (type == AppConstant.TYPE_COST_ENERGY) {
            ServerInterface.instance().queryCollegeSportsRankingData(student.getUniversityId(), pageSizeEnergy, pageNoEnergy, type, new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        try {
                            JSONArray rankingDataArray = json.getJSONObject("data").getJSONObject("university").getJSONObject("kcalConsumptionRanking").
                                    getJSONArray("data");
                            for (int i = 0; i < rankingDataArray.length(); i++) {
                                RankingData data = new RankingData();
                                data.setAvatar("");
                                data.setUserName(rankingDataArray.getJSONObject(i).getString("studentName"));
                                data.setCostValue(Integer.valueOf(rankingDataArray.getJSONObject(i).getString("kcalConsumption")));
                                data.setAvatar(rankingDataArray.getJSONObject(i).getString("avatarUrl"));
                                dataList.add(data);
                                DLOG.d(TAG, "onLoadMore  :    data:" + data);
                            }
                            adapter.notifyDataSetChanged();
                            return true;
                        } catch (org.json.JSONException e) {
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        DLOG.d(TAG, "errCode:" + errCode);
                        return false;
                    }
                }

            });

            if (pageNoEnergy != pageCountEnergy) {
                lrvLoadMore.loadMoreFinish(false, true);
            } else {
                lrvLoadMore.loadMoreFinish(false, false);
            }
            pageNoEnergy++;
        } else {
            ServerInterface.instance().queryCollegeSportsRankingData(student.getUniversityId(), pageSizeTime, pageNoTime, type, new JsonResponseCallback() {
                @Override
                public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                    if (errCode == 0) {
                        try {
                            JSONArray rankingDataArray = json.getJSONObject("data").getJSONObject("university").getJSONObject("timeCostedRanking").
                                    getJSONArray("data");
                            for (int i = 0; i < rankingDataArray.length(); i++) {
                                RankingData data = new RankingData();
                                data.setAvatar("");
                                data.setUserName(rankingDataArray.getJSONObject(i).getString("studentName"));
                                data.setCostValue(Integer.valueOf(rankingDataArray.getJSONObject(i).getString("timeCosted")));
                                data.setAvatar(rankingDataArray.getJSONObject(i).getString("avatarUrl"));
                                dataList.add(data);
                            }
                            adapter.notifyDataSetChanged();
                            return true;
                        } catch (org.json.JSONException e) {
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        DLOG.d(TAG, "errCode:" + errCode);
                        return false;
                    }
                }

            });
            if (pageNoTime != pageCountTime) {
                lrvLoadMore.loadMoreFinish(false, true);
            } else {
                lrvLoadMore.loadMoreFinish(false, false);
            }
            pageNoTime++;
        }
        // adapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
