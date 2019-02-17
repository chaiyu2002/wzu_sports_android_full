package com.tim.app.server.net;

import com.tim.app.RT;


/**
 * @author fanxiaofeng
 * @date 2016年3月28日
 * 动态域名
 */
public class ServerAddressManager {

    private static final String DEFAULT_BASE_ADDRESS = "https://api.guangyangyundong.com/";//默认的状态服务器地址

    /**
     * 获取服务器状态的域名
     *
     * @return
     */
    public static String getServerStateDomain() {
        String stateServerDomain = "";
        switch (RT.HOST) {
            case DEVELOP:
                stateServerDomain = "http://192.168.1.105:8080/";
                break;
            case DEBUG:
                stateServerDomain = "http://120.77.72.16:8080/";
                break;
            case PUBLISH:
                stateServerDomain = DEFAULT_BASE_ADDRESS;
                break;
        }
        return stateServerDomain;
    }


}
