package com.application.library.net;

/**
 * 不建议直接使用这个CallBack,
 * <p>
 * 可以使用对应的CallBack
 * <p>
 * String : StringResonseCallback
 * <p>
 * File : FileResponseCallback
 * <p>
 * Json : JsonResponseCallback
 * <p>
 * 
 * @author yangyu
 */
public abstract class  ResponseCallback
{
    public abstract boolean onResponse(Object result, int status, String errmsg, int id, boolean fromcache);

    /** get、post之前被调用 要用时重写方法 */
    public boolean onPreRequest()
    {
        return false;
    };

    public boolean onGetProcessing(int precent, long finished, long total)
    {
        return true;
    }

    public boolean onPostProcessing(int precent, long finished, long total)
    {
        return true;
    }
}
