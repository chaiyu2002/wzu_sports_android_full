package com.tim.app.server.api;

import com.application.library.log.DLOG;
import com.application.library.net.ResponseCallback;
import com.lzy.okhttputils.cache.CacheMode;
import com.tim.app.constant.AppConstant;
import com.tim.app.server.net.HttpMethod;
import com.tim.app.server.net.NetworkInterface;

import java.util.HashMap;

import static com.lzy.okhttputils.utils.OkLogger.tag;

/**
 * 接口
 */
public class ServerInterface {
    private static final String TAG = "ServerInterface";

    private static volatile ServerInterface instance = null;

    private static final String API_SCHEME = "api/";//扩展字段

    public static final String RUNNING_ACTIVITIES = "runningActivities";

    public static final String RUNNING_ACTIVITY_DATA = "runningActivityData";

    public static final String AREA_ACTIVITIES = "areaActivities";

    public static final String AREA_ACTIVITY_DATA = "areaActivityData";

    public static final String QUERY_INTERFACE = "graphql/query";

    public static final String TOKENS = "tokens";

    private String queryStr;

    private ServerInterface() {

    }

    public static ServerInterface instance() {
        if (instance == null) {
            synchronized (ServerInterface.class) {
                if (instance == null) {
                    instance = new ServerInterface();
                }
            }
        }
        return instance;
    }


    public void queryUniversities(ResponseCallback callback) {
        String queryStr = "{\n" +
                "  universities {\n" +
                "    id\n" +
                "    name\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    public void tokens(String tag, int universityId, String username, String password, String deviceId, int expiredHour, ResponseCallback callback) {
        String url = API_SCHEME + TOKENS;
        HashMap params = new HashMap();
        params.put("universityId", universityId);
        params.put("username", username);
        params.put("password", password);
        params.put("expiredHour", expiredHour);
        params.put("deviceId", deviceId);
        DLOG.d(TAG, "params: " + params);
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, false, callback);
    }

    public void queryStudent(int userId, ResponseCallback callback) {
        String queryStr = "{\n" +
                "  student(userId: " + userId + ") {\n" +
                "    id\n" +
                "    userId\n" +
                "    studentNo\n" +
                "    name\n" +
                "    isMan\n" +
                "    universityId\n" +
                "    classId\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    /**
     * 开始跑步
     * {@link com.tim.app.ui.activity.SportDetailActivity} 调用
     *
     * @param tag
     * @param runningSportId
     * @param studentId
     * @param startTime
     * @param callback
     */
    public void runningActivitiesStart(String tag, int runningSportId, int studentId, long startTime, ResponseCallback callback) {
        String url = API_SCHEME + RUNNING_ACTIVITIES + "/start";
        HashMap params = new HashMap();
        params.put("runningSportId", runningSportId);
        params.put("studentId", studentId);
        params.put("startTime", startTime);
        DLOG.d(TAG, "params: " + params);

        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, true, callback);
    }

    /**
     * 结束跑步运动
     * {@link com.tim.app.ui.activity.SportDetailActivity} 调用
     *
     * @param tag
     * @param distance           距离
     * @param costTime           花费时间
     * @param targetFinishedTime 目标时间
     * @param callback
     */
    public void runningActivitiesEnd(String tag, int id, int distance, int stepCount, long costTime, long targetFinishedTime,
                                     ResponseCallback callback) {
        String url = API_SCHEME + RUNNING_ACTIVITIES + "/end";
        HashMap params = new HashMap();
        params.put("id", id);
        params.put("distance", distance);
        params.put("stepCount", stepCount);
        params.put("costTime", costTime);
        params.put("targetFinishedTime", targetFinishedTime);
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, true, callback);
    }

    /**
     * 向服务器提交数据
     * {@link com.tim.app.ui.activity.SportDetailActivity}  调用
     */
    public void runningActivityData(String tag, int activityId, int stepCount, int stepCountCal, int distance, double longitude,
                                    double latitude, String distancePerStep, String stepPerSecond, int locationType, boolean isNormal, ResponseCallback callback) {
        String url = API_SCHEME + RUNNING_ACTIVITY_DATA;
        HashMap params = new HashMap();
        params.put("activityId", activityId);
        params.put("stepCount", stepCount);
        params.put("stepCountCal", stepCountCal);
        params.put("distance", distance);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        params.put("distancePerStep", distancePerStep);
        params.put("stepPerSecond", stepPerSecond);
        params.put("locationType", locationType);
        params.put("isNormal", isNormal);
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, true, callback);
    }

    /**
     * 创建本次区域运动项目
     *
     * @param tag
     * @param areaSportId
     * @param studentId
     * @param callback
     */
    public void areaActivities(String tag, int areaSportId, int studentId, int locationId, ResponseCallback callback) {
        String url = API_SCHEME + AREA_ACTIVITIES;
        HashMap params = new HashMap();
        params.put("areaSportId", areaSportId);
        params.put("studentId", studentId);
        params.put("locationId", locationId);
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, true, callback);
    }

    /**
     * 结束本次区域运动
     *
     * @param tag
     * @param areaSportRecordId
     * @param callback
     */
    public void areaActivitiesEnd(String tag, int areaSportRecordId, ResponseCallback callback) {
        String url = API_SCHEME + AREA_ACTIVITIES + "/" + areaSportRecordId;
        HashMap params = new HashMap();
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, true, callback);
    }

    /**
     * 提交区域运动数据
     *
     * @param tag
     * @param areaSportRecordId
     * @param callback
     */
    public void areaActivityData(String tag, int areaSportRecordId, double longitude,
                                 double latitude, int locationType, boolean isNormal, ResponseCallback callback) {
        String url = API_SCHEME + AREA_ACTIVITY_DATA;
        HashMap params = new HashMap();
        params.put("activityId", areaSportRecordId);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        params.put("locationType", locationType);
        params.put("isNormal", isNormal);
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, true, callback);
    }


    public void query(String queryStr, ResponseCallback callback) {
        String url = API_SCHEME + QUERY_INTERFACE;
        //        HttpHeaders headers = NetworkInterface.instance().getCommonHeaders();
        HashMap params = new HashMap();
        //        Log.d(TAG, "queryRunningSports: headers.toString()" + headers.toString());

        //        headers.put("Content-Type", "application/json;charset=UTF-8");
        //        Log.d(TAG, "headers: " + headers);
        //        headers.remove("content-type");
        //        Log.d(TAG, "headers: " + headers);
        //        headers.put("content-type", "x-www-form-urlencoded");
        //        Log.d(TAG, "headers: " + headers);
        params.put("query", queryStr);
        DLOG.d(TAG, "params: " + params);
        NetworkInterface.instance().connected(HttpMethod.POST, url, tag, params, CacheMode.DEFAULT, false, callback);
    }


    /**
     * MainActivity 调用
     *
     * @param universityId
     * @param callback
     */
    public void queryRunningSports(int universityId, boolean isEnabled, boolean isMan, ResponseCallback callback) {
        queryStr = "{\n" +
                "  runningSports(universityId:" + universityId + ",isEnabled: " + isEnabled + ",isMan:" + isMan + ") {\n" +
                "    acquisitionInterval\n" +
                "    participantNum\n" +
                "    id\n" +
                "    name\n" +
                "    isEnabled\n" +
                "    qualifiedDistance\n" +
                "    qualifiedCostTime\n" +
                "    imgUrl\n" +
                "    stepThreshold\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    /**
     * SportResultActivity 调用
     *
     * @param activityId
     * @param callback
     */
    public void queryRunningActivity(long activityId, ResponseCallback callback) {
        queryStr = "{\n" +
                "  runningActivity(id: " + activityId + ") {\n" +
                //                "    runningSportId\n" +
                //                "    studentId\n" +
                "    distance\n" +
                //                "    stepCount\n" +
                "    costTime\n" +
                //                "    targetFinishedTime\n" +
                //                "    sportDate\n" +
                //                "    startTime\n" +
                "    endedAt\n" +
                "    qualifiedDistance\n" +
                "    qualifiedCostTime\n" +
                //                "    minCostTime\n" +
                "    kcalConsumed\n" +
                "    qualified\n" +
                "    isValid\n" +
                "    isVerified\n" +
                //                "    speed\n" +
                //                "    stepPerSecond\n" +
                //                "    distancePerStep\n" +
                "    runningSport {\n" +
                "      name\n" +
                "    }\n" +
                "    data {\n" +
                "      longitude\n" +
                "      latitude\n" +
                "      isNormal\n" +
                "      locationType\n" +
                "      stepCount\n" +
                "      distance\n" +
                "      acquisitionTime\n" +
                "    }\n" +
                "  }\n" +
                "}";

        query(queryStr, callback);
    }

    public void queryAreaActivity(long activityId, ResponseCallback callback) {
        queryStr = "{\n" +
                "  areaActivity(id: " + activityId + ") {\n" +
                "    costTime\n" +
                "    qualified\n" +
                "    qualifiedCostTime\n" +
                "    kcalConsumed\n" +
                "    isValid\n" +
                "    isVerified\n" +
                "    endedAt\n" +
                // "    areaSport {\n" +
                // "      name\n" +
                // "    }\n" +
                "    data {\n" +
                "      longitude\n" +
                "      latitude\n" +
                "      locationType\n" +
                "      acquisitionTime\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    /**
     * MainActivity 调用
     *
     * @param universityId
     * @param studentId
     * @param callback
     */
    public void queryCurTermData(int universityId, int studentId, ResponseCallback callback) {
        String timeRange = "CURRENT_TERM";
        String queryStr = "{\n" +
                "  student(id: " + studentId + ") {\n" +
                "    areaActivityKcalConsumption\n" +
                "    runningActivityKcalConsumption\n" +
                "    areaActivityTimeCosted\n" +
                "    runningActivityTimeCosted\n" +
                "    currentTermQualifiedAreaActivityCount\n" +
                "    currentTermQualifiedRunningActivityCount\n" +
                "    currentTermAreaActivityCount\n" +
                "    currentTermRunningActivityCount\n" +
                "    signInCount(timeRange: " + timeRange + ")\n" +
                "  }\n" +
                "  university(id: " + universityId + ") {\n" +
                "    currentTerm {\n" +
                "      termSportsTask {\n" +
                "        targetSportsTimes\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    public void queryCollegeSportsRankingData(int universityId, int pageSize, int pageNo, int type, ResponseCallback callback) {
        if (type == AppConstant.TYPE_COST_ENERGY) {
            queryStr = "{    \n" +
                    "  university(id:" + universityId + ") {\n" +
                    "\t\tkcalConsumptionRanking (pageSize:" + pageSize + " pageNumber:" + pageNo + "){\n" +
                    "      pagesCount\n" +
                    "      data{\n" +
                    "      studentId\n" +
                    "      studentName\n" +
                    "      avatarUrl\n" +
                    "      kcalConsumption\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
        } else {
            queryStr = "{    \n" +
                    "  university(id:" + universityId + ") {\n" +
                    "\t\ttimeCostedRanking (pageSize:" + pageSize + " pageNumber:" + pageNo + "){\n" +
                    "      pagesCount\n" +
                    "      data{\n" +
                    "      studentId\n" +
                    "      studentName\n" +
                    "      timeCosted\n" +
                    "      avatarUrl\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
        }
        query(queryStr, callback);
    }

    public void queryTermInfo(int id, ResponseCallback callback) {
        queryStr = "{\n" +
                "term(id:" + id + "){\n" +
                "id\n" +
                "startDate\n" +
                "}\n" +
                "}";
        query(queryStr, callback);
    }

    public void queryHistorySportsRecord(int studentId, String startDate, String endDate, int type, ResponseCallback callback) {
        String timeRange = "";
        if (type == AppConstant.THIS_WEEK) {
            timeRange = "CURRENT_WEEK";
        } else if (type == AppConstant.THIS_MONTH) {
            timeRange = "CURRENT_MONTH";
        } else {
            timeRange = "CURRENT_TERM";
        }
        queryStr = "{\n" +
                "  student(id: " + studentId + ") {\n" +
                "    accuRunningActivityCount(timeRange: " + timeRange + ")\n" +
                "    accuAreaActivityCount(timeRange: " + timeRange + ")\n" +
                "    qualifiedRunningActivityCount(timeRange: " + timeRange + ")\n" +
                "    qualifiedAreaActivityCount(timeRange: " + timeRange + ")\n" +
                "    signInCount(timeRange: " + timeRange + ")\n" +
                "    runningActivityTimeCosted(timeRange: " + timeRange + ")\n" +
                "    areaActivityTimeCosted(timeRange: " + timeRange + ")\n" +
                "    runningActivityKcalConsumption(timeRange: " + timeRange + ")\n" +
                "    areaActivityKcalConsumption(timeRange: " + timeRange + ")\n" +
                "    runningActivities(startDate: \"" + startDate + "\", endDate: \"" + endDate + "\") {\n" +
                "      data {\n" +
                "        id\n" +
                "        runningSportId\n" +
                "        costTime\n" +
                "        distance\n" +
                "        kcalConsumed\n" +
                "        qualified\n" +
                "        startTime\n" +
                "        sportDate\n" +
                "        endedAt\n" +
                "        isValid\n" +
                "        isVerified\n" +
                "        runningSport {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    areaActivities(startDate: \"" + startDate + "\", endDate: \"" + endDate + "\") {\n" +
                "      data {\n" +
                "        id\n" +
                "        areaSportId\n" +
                "        location {\n" +
                "          name\n" +
                "          latitude\n" +
                "          isEnabled\n" +
                "          latitude\n" +
                "          longitude\n" +
                "          radius\n" +
                "          addr\n" +
                "        }\n" +
                "        areaSport {\n" +
                "          name\n" +
                "        }\n" +
                "        costTime\n" +
                "        kcalConsumed\n" +
                "        qualified\n" +
                "        isValid\n" +
                "        isVerified\n" +
                "        startTime\n" +
                "        sportDate\n" +
                "        endedAt\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    public void queryFitnessCheckData(int studentId, ResponseCallback callback) {
        String queryStr = "{\n" +
                "  student(id:" + studentId + ") {\n" +
                "    fitnessCheckDatas {\n" +
                "      height\n" +
                "      weight\n" +
                "      lungCapacity\n" +
                "      bmi\n" +
                "    }\n" +
                "  }\n" +
                "}";
        query(queryStr, callback);
    }

    public void queryAreaFixedLocationList(int universityId, ResponseCallback callback) {
        String queryStr = "{\n" +
                "  fixLocationOutdoorSportPoints(universityId: " + universityId + ") {\n" +
                "    id\n" +
                "    name\n" +
                "    description\n" +
                "    isEnabled\n" +
                "    latitude\n" +
                "    longitude\n" +
                "    radius\n" +
                "    addr\n" +
                "    qualifiedCostTime\n" +
                "    universityId\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    public void queryAppVersion(ResponseCallback callback) {
        String queryStr = "{\n" +
                "  latestVerison(platformId:0) {\n" +
                "    id\n" +
                "    versionName\n" +
                "    versionCode\n" +
                "    changeLog\n" +
                "    downloadUrl\n" +
                "    isForced\n" +
                "    platformId\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

    /**
     * 首页查询
     * {@link com.tim.app.ui.activity.MainActivity}调用
     *
     * @param universityId
     * @param callback
     */
    public void queryAreaSport(int universityId, ResponseCallback callback) {
        String queryStr = "{\n" +
                "  areaSports(universityId: " + universityId + ") {\n" +
                "    id\n" +
                "    name\n" +
                "    qualifiedCostTime\n" +
                "    acquisitionInterval\n" +
                "    isEnabled\n" +
                "    imgUrl\n" +
                "    participantNum\n" +
                "    universityId\n" +
                "  }\n" +
                "}\n";
        query(queryStr, callback);
    }

}
