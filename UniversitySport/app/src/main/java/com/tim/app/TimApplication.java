package com.tim.app;


import com.application.library.base.LSApplication;
import com.tencent.bugly.crashreport.CrashReport;

public class TimApplication extends LSApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        RT.application = this;
        RT.ins().init();

        CrashReport.initCrashReport(getApplicationContext(), "b1b01f99b1", true);
    }

}
