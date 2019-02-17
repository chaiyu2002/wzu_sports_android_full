package com.tim.app.util;

import android.content.res.Resources;

import com.tim.app.RT;

/**
 * @创建者 倪军
 * @创建时间 17/10/2017
 * @描述
 */

public class CommonUtils {

    public static int getColor(int resid) {
        return getResoure().getColor(resid);
    }

    public static Resources getResoure() {
        return RT.application.getResources();
    }

}
