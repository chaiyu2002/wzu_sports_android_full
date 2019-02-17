/*
 * Copyright 2013 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tim.app.sport;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;

import com.application.library.log.DLOG;
import com.application.library.runtime.event.EventManager;
import com.tim.app.constant.EventTag;
import com.tim.app.sport.entry.Acceleration;
import com.tim.app.sport.entry.Gravity;
import com.tim.app.sport.entry.Gyroscope;
import com.tim.app.ui.activity.SportDetailActivity;

import java.util.ArrayList;
import java.util.List;


/**
 * Background service which keeps the step-sensor listener alive to always get
 * the number of steps since boot.
 * <p/>
 * This service won't be needed any more if there is a way to read the
 * step-value without waiting for a sensor event
 */
public class SensorService extends Service implements SensorEventListener {

    private static final String TAG = "SensorService";
    private static SportDetailActivity sportDetailActivity = null;

    public static boolean stepCounterEnabled = false;

    private final static int NOTIFICATION_ID = 1;
    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000;
    //时间间隔
    private final static long SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HOUR;
    private final static int SAVE_OFFSET_STEPS = 1;

    public final static String ACTION_PAUSE = "pause";

    private static int steps;
    private static int lastSaveSteps;
    private static long lastSaveTime;
    private static long lastTime;

    /* 导入计步模块 */

    //存放三轴数据
    final int valueNum = 5;
    //用于存放计算阈值的波峰波谷差值
    float[] tempValue = new float[valueNum];
    int tempCount = 0;
    //是否上升的标志位
    boolean isDirectionUp = false;
    //持续上升次数
    int continueUpCount = 0;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    int continueUpFormerCount = 0;
    //上一点的状态，上升还是下降
    boolean lastStatus = false;
    //波峰值
    float peakOfWave = 0;
    //波谷值
    float valleyOfWave = 0;
    //此次波峰的时间
    long timeOfThisPeak = 0;
    //上次波峰的时间
    long timeOfLastPeak = 0;
    //当前的时间
    long timeOfNow = 0;
    //当前传感器的值
    float gravityNew = 0;
    //上次传感器的值
    float gravityOld = 0;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    final float initialValue = (float) 1.0;
    //初始阈值
    static float thresholdValue = (float) 4.0;
    //用x、y、z轴三个维度算出的平均值
    public static float average = 0;

    // 本算法使用的当前步数
    public static int stepCountCal = 0;
    // Step_Detector传感器使用的当前步数
    public static int detectorStep = 0;


    public Acceleration acceleration = null;
    public Gyroscope gyroscope = null;
    public Gravity gravity = null;
    public static List<Acceleration> accelerationList = new ArrayList<>();
    public static List<Gyroscope> gyroscopeList = new ArrayList<>();

    private double gravityArray[] = new double[3];
    private double linearAccelerationArray[] = new double[3];

    private static final float NS2S = 1.0f / 1000000000.0f;
    public final float[] deltaRotationVector = new float[4];
    private float timestamp;


    public final static String ACTION_UPDATE_NOTIFICATION = "updateNotificationState";

    public static float getThresholdValue() {
        return thresholdValue;
    }

    public static void setThresholdValue(float thresholdValue) {
        SensorService.thresholdValue = thresholdValue;
    }

    public static void setSportDetailActivity(SportDetailActivity sportDetailActivity) {
        SensorService.sportDetailActivity = sportDetailActivity;
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // nobody knows what happens here: step value might magically decrease
        // when this method is called...
    }

    /**
     * 这里的event中应该有最新的自启动以来的步数。
     *
     * @param event
     */
    @Override
    public void onSensorChanged(final SensorEvent event) {

            /*
            updateIfNecessary();
            if(lastTime == 0 || System.currentTimeMillis() - lastTime > MICROSECONDS_IN_ONE_MINUTE ) {
                // DLOG.d(TAG, "if sensor has changed, each minute insert into database.");
                Database.getInstance(this).saveDaySteps(steps);
                lastTime = System.currentTimeMillis();
            }
            */

        Sensor sensor = event.sensor;
        synchronized (this) {
            switch (sensor.getType()) {

                case Sensor.TYPE_STEP_COUNTER:
                    if (event.values[0] > Integer.MAX_VALUE) {
                        return;
                    } else {
                        steps = (int) event.values[0];
                        DLOG.d(TAG, "onSensorChanged: step counter: 计步 = " + steps);
                        sportDetailActivity.onStepChange(steps);
                    }
                    break;

                case Sensor.TYPE_ACCELEROMETER:
                    // DLOG.d("StepInAcceleration", "event.values[0]:" + event.values[0]);
                    // DLOG.d("StepInAcceleration", "event.values[1]:" + event.values[1]);
                    // DLOG.d("StepInAcceleration", "event.values[2]:" + event.values[2]);
                    float average = calcStep(event);

                    float alpha = 0.8f;

                    gravityArray[0] = alpha * gravityArray[0] + (1 - alpha) * event.values[0];
                    gravityArray[1] = alpha * gravityArray[1] + (1 - alpha) * event.values[1];
                    gravityArray[2] = alpha * gravityArray[2] + (1 - alpha) * event.values[2];

                    linearAccelerationArray[0] = event.values[0] - gravityArray[0];
                    linearAccelerationArray[1] = event.values[1] - gravityArray[1];
                    linearAccelerationArray[2] = event.values[2] - gravityArray[2];

                    acceleration = new Acceleration();
                    acceleration.setX((float) linearAccelerationArray[0]);
                    acceleration.setY((float) linearAccelerationArray[1]);
                    acceleration.setZ((float) linearAccelerationArray[2]);
                    acceleration.setAverage(average);
                    acceleration.setTimestamp(event.timestamp);
                    accelerationList.add(acceleration);

                    event.values[0] = (float) linearAccelerationArray[0];
                    event.values[1] = (float) linearAccelerationArray[1];
                    event.values[2] = (float) linearAccelerationArray[2];

                    // liteOrm.insert(acceleration);
                    break;
                case Sensor.TYPE_GRAVITY:
                    gravityArray[0] = event.values[0];
                    gravityArray[1] = event.values[1];
                    gravityArray[2] = event.values[2];

                    gravity.setX(event.values[0]);
                    gravity.setY(event.values[1]);
                    gravity.setZ(event.values[2]);
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    detectorStep++;
                    // saveAcceleration(event.timestamp, detectorStep);
                    DLOG.e("StepInAcceleration", "传感器计步： " + detectorStep++);
                    // EventManager.ins().sendEvent(EventTag.ON_DETECTOR_CHANGE, 0, 0, stepCountCal);

                    break;
                case Sensor.TYPE_GYROSCOPE:
                    // This time step's delta rotation to be multiplied by the current rotation
                    // after computing it from the gyro sample data.
                    if (timestamp != 0) {
                        final float dT = (event.timestamp - timestamp) * NS2S;
                        // Axis of the rotation sample, not normalized yet.
                        float axisX = event.values[0];
                        float axisY = event.values[1];
                        float axisZ = event.values[2];

                        // Calculate the angular speed of the sample
                        float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                        // Normalize the rotation vector if it's big enough to get the axis
                        // TODO
                        // if (omegaMagnitude > EPSILON) {
                        //     axisX /= omegaMagnitude;
                        //     axisY /= omegaMagnitude;
                        //     axisZ /= omegaMagnitude;
                        // }

                        float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                        deltaRotationVector[0] = sinThetaOverTwo * axisX;
                        deltaRotationVector[1] = sinThetaOverTwo * axisY;
                        deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                        deltaRotationVector[3] = cosThetaOverTwo;
                        gyroscope = new Gyroscope();
                        gyroscope.setAxisX(deltaRotationVector[0]);
                        gyroscope.setAxisY(deltaRotationVector[1]);
                        gyroscope.setAxisZ(deltaRotationVector[2]);
                        gyroscope.setAverage(deltaRotationVector[3]);
                        gyroscope.setTimestamp(event.timestamp);
                        gyroscopeList.add(gyroscope);
                    }
                    timestamp = event.timestamp;
                    float[] deltaRotationMatrix = new float[9];
                    SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                    // User code should concatenate the delta rotation we computed with the current rotation
                    // in order to get the updated rotation.
                    // rotationCurrent = rotationCurrent * deltaRotationMatrix;
                    break;
            }
        }
    }

    synchronized private float calcStep(SensorEvent event) {
        average = (float) Math.sqrt(Math.pow(event.values[0], 2)
                + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
        detectorNewStep(average);
        return average;
    }

    /*
         * 检测步子，并开始计步
         * 1.传入sersor中的数据
         * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
         * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
         * */
    public void detectorNewStep(float values) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (detectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();

                // TODO 每步的事件间隔。200 这个值需要调
                boolean a = timeOfNow - timeOfLastPeak >= 250;
                boolean b = (timeOfNow - timeOfLastPeak) <= 2000;
                boolean c = peakOfWave - valleyOfWave >= thresholdValue;
                // DLOG.d(TAG, "timeOfNow: " + timeOfNow);
                // DLOG.d(TAG, "timeOfLastPeak: " + timeOfLastPeak);
                // DLOG.d(TAG, "timeOfNow - timeOfLastPeak: " + (timeOfNow - timeOfLastPeak));
                // DLOG.d(TAG, "peakOfWave: " + peakOfWave);
                // DLOG.d(TAG, "valleyOfWave: " + valleyOfWave);
                // DLOG.d(TAG, "thresholdValue: " + thresholdValue);
                // DLOG.e(TAG, "peakOfWave - valleyOfWave: " + (peakOfWave - valleyOfWave));
                // DLOG.d(TAG, "a:" + a);
                // DLOG.d(TAG, "b:" + b);
                // DLOG.d(TAG, "c:" + c);

                if (a && b && c) {
                    timeOfThisPeak = timeOfNow;
                    // 更新界面的处理，不涉及到算法
                    stepCountCal++;
                    DLOG.e(TAG, "thresholdValue:" + thresholdValue + "计步：" + stepCountCal);
                    // EventManager.ins().sendEvent(EventTag.ON_ACCELERATION_CHANGE, 0, 0, stepCountCal);
                    sportDetailActivity.onStepCalcChange(stepCountCal);
                }
                if (timeOfNow - timeOfLastPeak >= 250
                        && (peakOfWave - valleyOfWave >= initialValue)) {
                    timeOfThisPeak = timeOfNow;
                    //                    thresholdValue = calcThresholdValue(peakOfWave - valleyOfWave);
                }
            }
        }
        gravityOld = values;
    }

    /**
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于1.2g,小于2g
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     *
     * @return true is peak,false is valley
     */
    public boolean detectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        // DLOG.d(TAG, "detectorPeak lastStatus: " + lastStatus);
        // DLOG.d(TAG, "detectorPeak newValue: " + newValue);
        // DLOG.d(TAG, "detectorPeak oldValue: " + oldValue);
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }
        // DLOG.d(TAG, "detectorPeak isDirectionUp: " + isDirectionUp);
        //        DLOG.d(TAG, "detectorPeak continueUpFormerCount: " + continueUpFormerCount);

        if (!isDirectionUp && lastStatus) {
            peakOfWave = oldValue;
            // DLOG.d(TAG, "peakOfWave:" + peakOfWave);
            return true;
            //上一次是向下，本次是向上
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            // DLOG.d(TAG, "valleyOfWave:" + valleyOfWave);
            return false;
        } else {
            return false;
        }
    }


    /**
     * 获取上次暂停时记录的步数，根据当前步数计算得出暂停时走的步数。插入新的记录到数据库，并且更新SP。
     * steps是自启动后的步数
     */
    private void updateIfNecessary() {
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {
            Database db = Database.getInstance(this);
            //为true 代表 今天的步数还未存入数据库中。
            if (db.getSteps(Util.getToday()) == Integer.MIN_VALUE) {
                int pauseDifference = steps -
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                .getInt("pauseCount", steps);
                db.insertNewDay(Util.getToday(), steps - pauseDifference);
                if (pauseDifference > 0) {
                    // update pauseCount for the new day
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("pauseCount", steps).commit();
                }
            }
            //保存自系统启动时到现在的步数
            DLOG.d(TAG, "自系统启动时到现在的步数steps:" + steps);
            db.saveCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();
            // updateNotificationState();

            Database db1 = Database.getInstance(this);
            // 上次启动时更新的步数 + 系统启动后到现在的步数 = 今天的总步数
            int steps = Math.max(db1.getCurrentSteps() + db1.getSteps(Util.getToday()), 0);
            db1.close();
            //内部会调用注册了的eventListener 的 handleMessage()方法
            EventManager.ins().sendEvent(EventTag.ON_STEP_CHANGE, 0, 0, steps);
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        /*
        if (intent != null && ACTION_PAUSE.equals(intent.getStringExtra("action"))) {
            // 开机
            if (steps == 0) {
                Database db = Database.getInstance(this);
                steps = db.getCurrentSteps();
                db.close();
            }
            SharedPreferences prefs = getSharedPreferences("pedometer", Context.MODE_PRIVATE);

            // TODO 这里对应的应该是拔掉插头，应该是没有
            if (prefs.contains("pauseCount")) { // resume counting，connect the power.
                int difference = steps -
                        prefs.getInt("pauseCount", steps); // number of steps taken during the pause
                // DLOG.d(TAG, "difference:" + difference);
                // DLOG.d(TAG, "steps：" + steps + "pauseCount:" + prefs.getInt("pauseCount", steps));
                Database db = Database.getInstance(this);
                db.addToLastEntry(-difference);
                db.close();
                prefs.edit().remove("pauseCount").apply();
                updateNotificationState();
            } else { // pause counting   // TODO 这里对应的应该是插上插头，这样stopSelf关闭服务是没有问题的
                // cancel restart
                ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE))
                        .cancel(PendingIntent.getService(getApplicationContext(), 2,
                                new Intent(this, SensorService.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));
                // 防止学生在充电的时候摇手机刷步数。
                prefs.edit().putInt("pauseCount", steps).apply();
                updateNotificationState();
                // TODO
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        // TODO 更新通知
        if (intent != null && intent.getBooleanExtra(ACTION_UPDATE_NOTIFICATION, false)) {
            updateNotificationState();
        } else {
            updateIfNecessary();
        }

        // restart service every hour to save the current step count
        ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, Math.min(Util.getTomorrow(),
                        System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR), PendingIntent
                        .getService(getApplicationContext(), 2,
                                new Intent(this, SensorService.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));
         */

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerSensor();
        acceleration = new Acceleration();
        gravity = new Gravity();
        gyroscope = new Gyroscope();
        // updateNotificationState();
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, SensorService.class), 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNotificationState() {
        //        if (BuildConfig.DEBUG) Logger.log("SensorService updateNotificationState");
        //        SharedPreferences prefs = getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        //        NotificationManager nm =
        //                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //        if (prefs.getBoolean("notification", true)) {
        //            int goal = prefs.getInt("goal", 10000);
        //            Database db = Database.getInstance(this);
        //            int today_offset = db.getSteps(Util.getToday());
        //            if (steps == 0)
        //                steps = db.getCurrentSteps(); // use saved value if we haven't anything better
        //            db.close();
        //            Notification.Builder notificationBuilder = new Notification.Builder(this);
        //            if (steps > 0) {
        //                if (today_offset == Integer.MIN_VALUE) today_offset = -steps;
        //                notificationBuilder.setProgress(goal, today_offset + steps, false).setContentText(
        //                        today_offset + steps >= goal ? getString(R.string.goal_reached_notification,
        //                                NumberFormat.getInstance(Locale.getDefault())
        //                                        .format((today_offset + steps))) :
        //                                getString(R.string.notification_text,
        //                                        NumberFormat.getInstance(Locale.getDefault())
        //                                                .format((goal - today_offset - steps))));
        //            } else { // still no step value?
        //                notificationBuilder
        //                        .setContentText(getString(R.string.your_progress_will_be_shown_here_soon));
        //            }
        //            boolean isPaused = prefs.contains("pauseCount");
        //            notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
        //                    .setContentTitle(isPaused ? getString(R.string.ispaused) :
        //                            getString(R.string.notification_title)).setContentIntent(PendingIntent
        //                    .getActivity(this, 0, new Intent(this, Activity_Main.class),
        //                            PendingIntent.FLAG_UPDATE_CURRENT))
        //                    .setSmallIcon(R.drawable.ic_notification)
        //                    .addAction(isPaused ? R.drawable.ic_resume : R.drawable.ic_pause,
        //                            isPaused ? getString(R.string.resume) : getString(R.string.pause),
        //                            PendingIntent.getService(this, 4, new Intent(this, SensorService.class)
        //                                            .putExtra("action", ACTION_PAUSE),
        //                                    PendingIntent.FLAG_UPDATE_CURRENT)).setOngoing(true);
        //            nm.notify(NOTIFICATION_ID, notificationBuilder.build());
        //        } else {
        //            nm.cancel(NOTIFICATION_ID);
        //        }
    }

    private void registerSensor() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        boolean isAvailable = false;


        Sensor stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCountSensor != null) {
            isAvailable = sensorManager.registerListener(this, stepCountSensor,
                    SensorManager.SENSOR_DELAY_NORMAL, (int) (5 * MICROSECONDS_IN_ONE_MINUTE));
            if (isAvailable) {
                DLOG.v(TAG, "STEP_COUNTER传感器可以使用");
                stepCounterEnabled = true;
            } else {
                DLOG.v(TAG, "STEP_COUNTER传感器无法使用");
                stepCounterEnabled = false;
            }
            // } else {
            //     DLOG.v(TAG, "手机不支持STEP_COUNTER传感器");
            //     Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //     if (accelerometerSensor != null) {
            //         isAvailable = sensorManager.registerListener(this, accelerometerSensor,
            //                 SensorManager.SENSOR_DELAY_UI);
            //         if (isAvailable) {
            //             DLOG.v(TAG, "ACCELEROMETER传感器可以使用");
            //         } else {
            //             DLOG.v(TAG, "ACCELEROMETER传感器无法使用");
            //         }
            //     } else {
            //         DLOG.v(TAG, "ACCELEROMETER传感器无法使用");
            //     }
        }

        // 现在暂时测试，即使有STEP_COUNTER传感器也打开加速度传感器
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            isAvailable = sensorManager.registerListener(this, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_UI);
            if (isAvailable) {
                DLOG.v(TAG, "ACCELEROMETER传感器已开启");
            } else {
                DLOG.v(TAG, "ACCELEROMETER传感器无法开启");
            }
        } else {
            DLOG.v(TAG, "没有ACCELEROMETER传感器");
        }

        Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gravitySensor != null) {
            isAvailable = sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_UI);
            if (isAvailable) {
                DLOG.v(TAG, "GRAVITY传感器可以使用");
            } else {
                DLOG.v(TAG, "GRAVITY传感器无法使用");
            }
        } else {
            DLOG.v(TAG, "GRAVITY传感器无法使用");
        }

        // 陀螺仪
        // Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // isAvailable = sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI);
        // if (isAvailable) {
        //     DLOG.v(TAG, "GYROSCOPE传感器可以使用");
        //     isAvailable = true;
        // } else {
        //     DLOG.v(TAG, "GYROSCOPE传感器无法使用");
        //     isAvailable = false;
        // }

        // detector 可能导致耗电加快
        // Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        // if (detectorSensor != null) {
        //     isAvailable = sensorManager.registerListener(this, detectorSensor, SensorManager.SENSOR_DELAY_UI);
        //     if (isAvailable) {
        //         DLOG.v(TAG, "STEP_DETECTOR传感器可以使用");
        //     } else {
        //         DLOG.v(TAG, "STEP_DETECTOR传感器无法使用");
        //     }
        // } else {
        //     DLOG.v(TAG, "STEP_DETECTOR传感器无法使用");
        // }
    }
}
