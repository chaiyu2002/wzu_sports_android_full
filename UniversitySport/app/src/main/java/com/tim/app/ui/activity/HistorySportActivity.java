package com.tim.app.ui.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatImageView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.application.library.util.SmoothSwitchScreenUtil;
import com.tim.app.R;
import com.tim.app.constant.AppConstant;
import com.tim.app.ui.adapter.TabAdapter;
import com.tim.app.ui.fragment.HistoryDataFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史运动数据
 */
public class HistorySportActivity extends ToolbarActivity {

    private static final String TAG = "HistorySportActivity";

    private TabLayout tabLayout;
    private ViewPager vpHistoryData;
    private TabAdapter pagerAdapter;

    private HistoryDataFragment fragmentWeek;
    private HistoryDataFragment fragmentMonth;
    private HistoryDataFragment fragmentTerm;

    public int currentState = -1;
    private List<HistoryDataFragment> historySportFragmentList = new ArrayList<>();

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
        SmoothSwitchScreenUtil.smoothSwitchScreen(this);
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        setOverflowButtonColor(this);
    }

    public static void setOverflowButtonColor(final Activity activity) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
                overflow.setColorFilter(Color.WHITE);
                overflow.setImageResource(R.drawable.ic_option);
                removeOnGlobalLayoutListener(decorView, this);
            }
        });
    }

    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    @Override
    public void initView() {
        tabLayout = (TabLayout) findViewById(R.id.stbNavBar);
        vpHistoryData = (ViewPager) findViewById(R.id.vpHistoryData);

        pagerAdapter = new TabAdapter(getSupportFragmentManager());
        fragmentWeek = HistoryDataFragment.newInstance(AppConstant.TYPE_WEEK, AppConstant.STATUS_ALL);
        fragmentMonth = HistoryDataFragment.newInstance(AppConstant.TYPE_MONTH, AppConstant.STATUS_ALL);
        fragmentTerm = HistoryDataFragment.newInstance(AppConstant.TYPE_TERM, AppConstant.STATUS_ALL);
        historySportFragmentList.add(fragmentWeek);
        historySportFragmentList.add(fragmentMonth);
        historySportFragmentList.add(fragmentTerm);
        pagerAdapter.addFragment(fragmentWeek, "本周");
        pagerAdapter.addFragment(fragmentMonth, "本月");
        pagerAdapter.addFragment(fragmentTerm, "本学期");
        vpHistoryData.setOffscreenPageLimit(2);//除去当前显示页面外，还需要预先加载的页面个数
        vpHistoryData.setAdapter(pagerAdapter);
        vpHistoryData.setCurrentItem(0);
        vpHistoryData.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                historySportFragmentList.get(position).currentFragmentState = currentState;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout.setupWithViewPager(vpHistoryData);
    }

    @Override
    public void initData() {
        setTitle(getString(R.string.app_history));
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
    protected int getLayoutId() {
        return R.layout.activity_history;
    }

    public interface PageSelectListener {
        void onPageSelected(int status);
    }

    public int selectId = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        //menu.setGroupCheckable(R.id.option_menu, true, true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setCheckable(false);
            menu.getItem(i).setChecked(false);
        }
        menu.getItem(selectId).setCheckable(true);
        menu.getItem(selectId).setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //supportInvalidateOptionsMenu();
        for (HistoryDataFragment fragment : historySportFragmentList) {
            if (fragment.isInit()) {
                fragment.changeHistoryDataList(item, -1);
                item.setCheckable(true);
                item.setChecked(true);
                switch (item.getItemId()) {
                    case R.id.action_all:
                        selectId = 0;
                        break;
                    case R.id.action_qualified:
                        selectId = 1;
                        break;
                    case R.id.action_disqualified:
                        selectId = 2;
                        break;
//                    case R.id.action_not_verified:
//                        selectId = 3;
//                        break;
                    case R.id.action_verified_fail:
                        selectId = 3;//4->3
                        break;
//                    case R.id.action_abnormal_end:
//                        selectId = 5;
//                        break;
                    default:break;
                }
            }
        }
        return true;

    }

}
