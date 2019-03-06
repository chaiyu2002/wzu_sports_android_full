# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\leshow\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#指定代码的压缩级别
-optimizationpasses 5
#包明不混合大小写
-dontusemixedcaseclassnames
#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
#预校验
-dontpreverify
#混淆时是否记录日志
-dontwarn
-verbose
#优化 不优化输入的类文件
-dontoptimize
#忽略警告
-ignorewarnings
#保护注解
-keepattributes *Annotation*
#混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#apk 包内所有 class 的内部结构
#-dump class_files.txt
#未混淆的类和成员
#-printseeds seeds.txt
#列出从 apk 中删除的代码
#-printusage unused.txt
#混淆前后的映射
#-printmapping mapping.txt
#保持哪些类不被混淆
-keep class * extends android.app.Dialog
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.widget.LinearLayout
-keep public class * extends android.widget.RelativeLayout
-keep public class com.android.vending.licensing.ILicensingService
-keep public class android.support.**{
	<methods>;
	<fields>;
}

-keep class **.R$* {   *;  }
-keep class com.lzy.okhttputils.**
-keep class com.lzy.okhttputils.** { *; }
-keep class com.application.library.** { *; }
-keep class com.nostra13.universalimageloader.** { *; }


-keep class com.google.**{*;}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep public class * implements java.io.Serializable {*;}


-dontshrink
-dontoptimize
-dontwarn com.google.android.maps.**
-dontwarn android.webkit.WebView
-keep public class javax.**
-keep public class android.webkit.**
-dontwarn android.support.v4.**
-keepattributes Exceptions,InnerClasses,Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keep class com.linkedin.** { *; }
-keep class com.android.dingtalk.share.ddsharemodule.** { *; }
-keepattributes Signature

 -dontwarn sun.misc.**

 -keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
 }

 -keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
     rx.internal.util.atomic.LinkedQueueNode producerNode;
 }

 -keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
     rx.internal.util.atomic.LinkedQueueNode consumerNode;
 }

 -keepattributes Exceptions,InnerClasses

 # Keep names - Native method names. Keep all native class/method names.
 -keepclasseswithmembers,allowshrinking class * {
     native <methods>;
 }

 -keepclasseswithmembers,allowshrinking class * {
     public <init>(android.content.Context,android.util.AttributeSet);
 }

 -keepclasseswithmembers,allowshrinking class * {
     public <init>(android.content.Context,android.util.AttributeSet,int);
 }

 -keepclassmembers enum  * {
     public static **[] values();
     public static ** valueOf(java.lang.String);
 }

 -ignorewarning
 -keep public class * extends android.widget.TextView

 #-----------keep httpclient -------------------
 -keep class org.apache.** {
     public <fields>;
     public <methods>;
 }





 #--------------alipay-------------
 -keep class com.ta.utdid2.** {
     public <fields>;
     public <methods>;
 }
 -keep class com.ut.device.** {
     public <fields>;
     public <methods>;
 }
 -keep class HttpUtils.** {
     public <fields>;
     public <methods>;
 }

#畅言所有权限-------------------------------start------------------------------------------

#-libraryjars libs/android-support-v4.jar

-dontwarn com.avos.avoscloud.**

-keep class android.net.http.** { *;}

-keep class android.content.** { *; }

-keep class android.support.v4.** { *; }
-keep class com.google.gson.JsonObject { *; }
-keep class com.badlogic.** { *; }
-keep class * implements com.badlogic.gdx.utils.Json*
-keep class com.google.** { *; }

-keepattributes Signature

-keepclasseswithmembernames class * {                                           # 保持 native 方法不被混淆
    native <methods>;
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context);
}


-keepclasseswithmembers class * {                                               # 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);     # 保持自定义控件类不被混淆
}

-keepclassmembers class * extends android.app.Activity {                        # 保持自定义控件类不被混淆
   public void *(android.view.View);
}

-keepclassmembers enum * {                                                      # 保持枚举 enum 类不被混淆
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {                                # 保持 Parcelable 不被混淆
  public static final android.os.Parcelable$Creator *;
}


-keepclassmembers class com.sohu.cyan.android.sdk.activity.ShareActivity {
	public *;
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

#保护指定的类文件和类的成员
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}


# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule


#3D 地图 V5.0.0之前：
-keep   class com.amap.api.maps.**{*;}
-keep   class com.autonavi.amap.mapcore.*{*;}
-keep   class com.amap.api.trace.**{*;}

#3D 地图 V5.0.0之后：
-keep   class com.amap.api.maps.**{*;}
-keep   class com.autonavi.**{*;}
-keep   class com.amap.api.trace.**{*;}

#定位
-keep class com.amap.api.location.**{*;}
-keep class com.amap.api.fence.**{*;}
-keep class com.autonavi.aps.amapapi.model.**{*;}

#搜索
-keep   class com.amap.api.services.**{*;}

#2D地图
-keep class com.amap.api.maps2d.**{*;}
-keep class com.amap.api.mapcore2d.**{*;}

#导航
-keep class com.amap.api.navi.**{*;}
-keep class com.autonavi.**{*;}

#
--keep class com.autonavi.**{*;}

-keep class com.tencent.mm.opensdk.** {
    *;
}

-keep class com.tencent.wxop.** {
    *;
}

-keep class com.tencent.mm.sdk.** {
    *;
}

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}