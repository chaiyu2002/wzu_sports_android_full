package com.tim.app.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.ImageButton;

import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.util.SmoothSwitchScreenUtil;
import com.application.library.widget.EmptyLayout;
import com.application.library.widget.loadmore.LoadMoreContainer;
import com.application.library.widget.loadmore.LoadMoreHandler;
import com.application.library.widget.loadmore.LoadMoreRecycleViewContainer;
import com.application.library.widget.recycle.BaseRecyclerAdapter;
import com.application.library.widget.recycle.HorizontalDividerItemDecoration;
import com.application.library.widget.recycle.WrapRecyclerView;
import com.tim.app.R;
import com.tim.app.RT;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.FixLocationOutdoorSportPoint;
import com.tim.app.server.entry.SportEntry;
import com.tim.app.ui.adapter.SportAreaListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.tim.app.constant.AppConstant.student;

/**
 * 运动区域
 */
public class SportsAreaListActivity extends BaseActivity implements LoadMoreHandler, BaseRecyclerAdapter.OnItemClickListener {

    private static final String TAG = "SportsAreaListActivity";
    private SportsAreaListActivity context;
    private ImageButton ibBack;

    private LoadMoreRecycleViewContainer lrvLoadMore;
    private WrapRecyclerView wrvArea;
    private EmptyLayout emptyLayout;

    private SportAreaListAdapter adapter;
    private List<FixLocationOutdoorSportPoint> dataList;
    private SportEntry sportEntry;

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
        context = this;
    }

    public static void start(Context context, SportEntry sportEntry) {
        Intent intent = new Intent(context, SportsAreaListActivity.class);
        intent.putExtra("sportEntry", sportEntry);
        context.startActivity(intent);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sports_area_list;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        sportEntry = (SportEntry) getIntent().getSerializableExtra("sportEntry");
    }

    @Override
    public void initView() {
        ibBack = (ImageButton) findViewById(R.id.ibBack);
        ibBack.setOnClickListener(this);

        lrvLoadMore = (LoadMoreRecycleViewContainer) findViewById(R.id.lrvLoadMore);
        wrvArea = (WrapRecyclerView) findViewById(R.id.wrvArea);
        wrvArea.setOverScrollMode(View.OVER_SCROLL_NEVER);

        lrvLoadMore.useDefaultFooter(View.GONE);
        lrvLoadMore.setAutoLoadMore(true);
        lrvLoadMore.setLoadMoreHandler(this);

        emptyLayout = new EmptyLayout(this, lrvLoadMore);
        emptyLayout.showLoading();
        emptyLayout.setEmptyButtonShow(false);
        emptyLayout.setErrorButtonShow(true);
        emptyLayout.setEmptyDrawable(R.drawable.ic_empty_score_data);
        emptyLayout.setEmptyText("没有锻炼区域可供选择");
        emptyLayout.setEmptyButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emptyLayout.showLoading();
                initData();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        wrvArea.setLayoutManager(layoutManager);

        wrvArea.addItemDecoration((new HorizontalDividerItemDecoration.Builder(
                this).color(getResources().getColor(R.color.transparent)).size((int) (RT.getDensity() * 1)).build()));

        dataList = new ArrayList<>();
        adapter = new SportAreaListAdapter(this, dataList);
        wrvArea.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }


    @Override
    public void initData() {
        queryAreaSportData();
    }


    /**
     * 查询区域运动记录
     */
    public void queryAreaSportData() {
        ServerInterface.instance().queryAreaFixedLocationList(student.getUniversityId(), new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    JSONArray sportArray = json.optJSONObject("data").optJSONArray("fixLocationOutdoorSportPoints");
                    try {
                        for (int i = 0; i < sportArray.length(); i++) {
                            JSONObject jsonObject = sportArray.getJSONObject(i);
                            if (jsonObject.optBoolean("isEnabled")){
                                FixLocationOutdoorSportPoint fixLocationOutdoorSportPoint = new FixLocationOutdoorSportPoint();
                                fixLocationOutdoorSportPoint.setEnabled(jsonObject.optBoolean("isEnabled"));
                                fixLocationOutdoorSportPoint.setId(jsonObject.optInt("id"));
                                fixLocationOutdoorSportPoint.setAreaName(jsonObject.optString("name"));
                                fixLocationOutdoorSportPoint.setDescription(jsonObject.optString("description"));
                                fixLocationOutdoorSportPoint.setAddress(jsonObject.optString("addr"));
                                fixLocationOutdoorSportPoint.setLatitude(jsonObject.optDouble("latitude"));
                                fixLocationOutdoorSportPoint.setLongitude(jsonObject.optDouble("longitude"));
                                fixLocationOutdoorSportPoint.setRadius(jsonObject.optDouble("radius"));
                                fixLocationOutdoorSportPoint.setQualifiedCostTime(jsonObject.optInt("qualifiedCostTime"));
                                fixLocationOutdoorSportPoint.setUniversityId(jsonObject.optInt("universityId"));
                                dataList.add(fixLocationOutdoorSportPoint);
                                wrvArea.setAdapter(adapter);
                                adapter.setOnItemClickListener(context);
                                adapter.notifyDataSetChanged();
                                if (dataList.size() == 0) {
                                    emptyLayout.showEmpty();
                                } else {
                                    emptyLayout.showContent();
                                    lrvLoadMore.loadMoreFinish(false, false);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        emptyLayout.showEmpty();
                        e.printStackTrace();
                    }
                    return true;
                } else {
                    DLOG.d(TAG, "onJsonResponse: errcode = " + errCode);
                    return false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SmoothSwitchScreenUtil.smoothSwitchScreen(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibBack:
                finish();
                break;
        }
    }

    @Override
    public void onLoadMore(LoadMoreContainer loadMoreContainer) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onItemClick(View view, int position, long id) {
        //        SportPrepareActivity.start(this,dataList.get(position),sportEntry,true);
        /**
         * 这里的 mAreaSportEntryList 从现在服务端只提供了一个
         */
        SportFixedLocationActivity.start(this, dataList.get(position), sportEntry);
    }
}
