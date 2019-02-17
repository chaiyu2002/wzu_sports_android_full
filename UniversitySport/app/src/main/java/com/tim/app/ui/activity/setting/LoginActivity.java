package com.tim.app.ui.activity.setting;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.application.library.log.DLOG;
import com.application.library.net.JsonResponseCallback;
import com.application.library.runtime.event.EventManager;
import com.application.library.util.NetUtils;
import com.lzy.okhttputils.OkHttpUtils;
import com.lzy.okhttputils.model.HttpHeaders;
import com.tim.app.R;
import com.tim.app.RT;
import com.tim.app.constant.AppConstant;
import com.tim.app.constant.EventTag;
import com.tim.app.server.api.ServerInterface;
import com.tim.app.server.entry.Student;
import com.tim.app.server.entry.University;
import com.tim.app.server.entry.User;
import com.tim.app.server.net.NetworkInterface;
import com.tim.app.ui.activity.BaseActivity;
import com.tim.app.ui.activity.MainActivity;
import com.tim.app.ui.dialog.ProgressDialog;
import com.tim.app.util.EncryptUtil;
import com.tim.app.util.SoftKeyboardUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.tim.app.constant.AppConstant.student;
import static com.tim.app.constant.AppConstant.user;
import static com.tim.app.ui.activity.SportDetailActivity.NETWORK_ERROR_MSG;

/**
 * 登录
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    public static final String USER_IS_FIRST_LOGIN = "user_is_first_login";
    public static final String USER_HAS_EDIT_FIRST_PASSWORD = "user_has_edit_first_password";
    private EditText etStudentNo, etPassword;
    private Button btLogin;
    private TextView tvNoErrorPrmpt;
    private TextView tvPasswordErrorPrmpt;
    private TextView tvUniErrorPrmpt;
    private ImageView ivPasswordVisiable;
    private TextView tvForgotPassword;
    private ImageView ivDeleteNo;
    private ImageView ivPasswordDelete;
    private boolean mHasEditFirstPassword;
    private TextView tvUniversity;
    private ProgressDialog progressDialog;

    private List<University> universities = new ArrayList<>();
    private List<String> universityNames = new ArrayList<>();

    // TODO
    //    public static User user;
    //    public static Student student;

    private Context context = this;
    private String deviceId;

    //TODO
    private int expirationTime = 2160;

    @Override
    protected void onBeforeSetContentLayout() {
        super.onBeforeSetContentLayout();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    public void initView() {
        btLogin = (Button) findViewById(R.id.btLogin);
        etStudentNo = (EditText) findViewById(R.id.etStudentNo);
        tvUniversity = (TextView) findViewById(R.id.tvUniversity);
        etPassword = (EditText) findViewById(R.id.etPassword);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // etStudentNo.setText("nijun");
        // etStudentNo.setText("15211040107");
        // etStudentNo.setText("15211031102");
        // etPassword.setText("123456");
        // queryUniversities();

        if (!NetUtils.isConnection(this)) {
            Toast.makeText(context, getString(R.string.httpconnection_not_network), Toast.LENGTH_SHORT).show();
        }

        tvNoErrorPrmpt = (TextView) findViewById(R.id.tvStuNoErrorPrmpt);
        tvPasswordErrorPrmpt = (TextView) findViewById(R.id.tvPasswordErrorPrmpt);
        tvUniErrorPrmpt = (TextView) findViewById(R.id.tvUniErrorPrmpt);
        ivPasswordVisiable = (ImageView) findViewById(R.id.ivPasswordVisiable);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        ivDeleteNo = (ImageView) findViewById(R.id.ivDeleteNo);
        ivPasswordDelete = (ImageView) findViewById(R.id.ivPasswordDelete);
        tvForgotPassword.setOnClickListener(this);
        findViewById(R.id.ibClose).setOnClickListener(this);
        findViewById(R.id.ivDeleteNo).setOnClickListener(this);
        findViewById(R.id.ivPasswordDelete).setOnClickListener(this);
        findViewById(R.id.tvForgotPassword).setOnClickListener(this);
        findViewById(R.id.ivPasswordVisiable).setOnClickListener(this);
        btLogin.setOnClickListener(this);
        tvUniversity.setOnClickListener(this);

        etStudentNo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() > 0) {
                    ivDeleteNo.setVisibility(View.VISIBLE);
                } else {
                    ivDeleteNo.setVisibility(View.GONE);
                }
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length() > 0) {
                    ivPasswordDelete.setVisibility(View.VISIBLE);
                } else {
                    ivPasswordDelete.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void initData() {
        //同时获取Android_ID
        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        DLOG.d(TAG + "deviceId", deviceId);
    }

    private void queryUniversities() {
        ServerInterface.instance().queryUniversities(new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    try {
                        JSONArray jsonArray = json.getJSONObject("data").getJSONArray("universities");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            University university = new University();

                            university.setId(jsonObject.getInt("id"));
                            String name = jsonObject.getString("name");
                            university.setName(name);

                            universityNames.add(name);
                            universities.add(university);
                        }
                        progressDialog.dismissCurrentDialog();
                        showUniversityDialog();
                        return true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        progressDialog.dismissCurrentDialog();
                        Toast.makeText(LoginActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    progressDialog.dismissCurrentDialog();
                    Toast.makeText(LoginActivity.this, NETWORK_ERROR_MSG, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibClose:
                finish();
                break;
            case R.id.ivDeleteNo:
                etStudentNo.setText("");
                break;
            case R.id.ivPasswordDelete:
                etPassword.setText("");
                break;
            case R.id.tvForgotPassword:
                findPassword();
                break;
            case R.id.btLogin:
                String username = etStudentNo.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (!checkLogin(username, password)) {
                    //客户端初步验证不通过
                } else {
                    login(username, password);
                }
                break;
            case R.id.ivPasswordVisiable:
                ivPasswordVisiable.setSelected(!ivPasswordVisiable.isSelected());
                if (ivPasswordVisiable.isSelected()) {
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                break;
            case R.id.tvUniversity:
                progressDialog.show();

                if (universityNames.size() == 0) {
                    queryUniversities();
                } else {
                    showUniversityDialog();
                }
                break;
        }
    }

    private void showUniversityDialog() {
        final CharSequence[] names = universityNames.toArray(new CharSequence[universityNames.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("请选择您的学校");
        builder.setItems(names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // the user clicked on colors[which]
                tvUniversity.setText(names[which]);
                DLOG.d(TAG, "which:" + which);
                DLOG.d(TAG, "names[which]:" + names[which]);
                tvUniversity.setTag(which);
            }
        });

        progressDialog.dismissCurrentDialog();
        builder.show();
    }


    /**
     * 检测登陆参数
     *
     * @param sNo
     * @param password
     * @return
     */
    public boolean checkLogin(String sNo, String password) {

        if (TextUtils.isEmpty(tvUniversity.getText())) {
            tvUniErrorPrmpt.setVisibility(View.VISIBLE);
            tvUniErrorPrmpt.setText(RT.getString(R.string.select_your_university));
            return false;
        } else {
            tvUniErrorPrmpt.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(sNo)) {
            tvNoErrorPrmpt.setVisibility(View.VISIBLE);
            tvNoErrorPrmpt.setText(RT.getString(R.string.login_input_num));
            return false;
        } else {
            tvNoErrorPrmpt.setVisibility(View.GONE);
        }

        //        if (!sNo.matches(StringUtil.ZHENGZE_SNO)) {
        //            tvNoErrorPrmpt.setVisibility(View.VISIBLE);
        //            tvNoErrorPrmpt.setText(RT.getString(R.string.error_sno_error));
        //            return false;
        //        } else {
        //            tvNoErrorPrmpt.setVisibility(View.GONE);
        //        }

        if (TextUtils.isEmpty(password)/* || !password.matches(StringUtil.ZHENGZE_PASSWORD)*/) {
            tvPasswordErrorPrmpt.setVisibility(View.VISIBLE);
            tvPasswordErrorPrmpt.setText(RT.getString(R.string.error_password_noput));
            return false;
        } else {
            tvPasswordErrorPrmpt.setVisibility(View.GONE);
        }
        return true;
    }

    private void queryStudent(int uid) {
        ServerInterface.instance().queryStudent(uid, new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    JSONObject jsonObject = json.optJSONObject("data").optJSONObject("student");
                    student = new Student();
                    student.setId(jsonObject.optInt("id"));
                    student.setUniversityId(jsonObject.optInt("universityId"));
                    student.setName(jsonObject.optString("name"));
                    student.setStudentNo(jsonObject.optString("studentNo"));
                    student.setClassId(jsonObject.optInt("classId"));
                    student.setUserId(jsonObject.optInt("userId"));
                    student.setMan(jsonObject.optBoolean("isMan"));
                    user.setStudent(student);

                    DLOG.d(TAG, "已找到用户ID为：" + user.getUid() + "的学生信息，学号为" + student.getId() + "，姓名为" + student.getName());
                    saveUser(User.USER_SHARED_PREFERENCE, User.USER, user);
                    MainActivity.start(context);

                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void login(final String username, final String password) {
        showLoadingDialog();

        final String md5Password = EncryptUtil.md5(password);
        //判断选择的是哪所学校
        int index = (int) tvUniversity.getTag();
        University university = universities.get(index);
        //        Log.d(TAG, "universities.get(index):" + universities.get(index));
        ServerInterface.instance().tokens(TAG, university.getId(), username, md5Password, deviceId, expirationTime, new JsonResponseCallback() {
            @Override
            public boolean onJsonResponse(JSONObject json, int errCode, String errMsg, int id, boolean fromCache) {
                if (errCode == 0) {
                    if (user == null) {
                        user = new User();
                    }

                    JSONObject jsonObject = json.optJSONObject("obj");
                    if (jsonObject == null) {
                        hideLoadingDialog();
                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    try {
                        user.setUid(jsonObject.getInt("userId"));
                        List<String> roles = new ArrayList<>();
                        JSONArray roleArray = jsonObject.getJSONArray("roles");
                        for (int i = 0; i < roles.size(); i++) {
                            roles.add(roleArray.optString(i));
                        }
                        user.setRoles(roles.toArray(new String[roles.size()]));

                        user.setExpiredDate(jsonObject.getLong("expiredDate"));

                        String token = jsonObject.getString("token");
                        user.setToken(token);

                        user.setAvatarUrl(jsonObject.getString("avatarUrl"));

                        //添加token 至 HttpHeader
                        HttpHeaders headers = NetworkInterface.instance().getCommonHeaders();
                        headers.put("Authorization", user.getToken());
                        NetworkInterface.instance().setCommonHeaders(headers);
                        DLOG.d(TAG, "headers:" + headers);

                        user.setUsername(username);
                        user.setPassword(md5Password);

                        queryStudent(user.getUid());

                        return true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        hideLoadingDialog();
                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    // TODO or other Runtime Exception(Uncheck Exception).
                } else {
                    //TODO 密码或者用户名不匹配，网络接口返回不明确
                    hideLoadingDialog();
                    Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    /**
     * 保存到SharedPreferences
     *
     * @param user
     */
    private void saveUser(String preferenceName, String key, User user) {
        SharedPreferences.Editor editor = getSharedPreferences(preferenceName, MODE_PRIVATE).edit();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(user);
            String temp = new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));
            //            Log.d(TAG, "temp: " + temp);

            String value = getSharedPreferences(preferenceName, MODE_PRIVATE).getString(key, "");
            //            Log.d(TAG, "value: " + value);

            editor.remove(key);

            value = getSharedPreferences(preferenceName, MODE_PRIVATE).getString(key, "");
            //            Log.d(TAG, "after remove value: " + value);

            editor.putString(key, temp);

            value = getSharedPreferences(preferenceName, MODE_PRIVATE).getString(key, "");
            //            Log.d(TAG, "after put value: " + value);

            editor.putBoolean(AppConstant.IS_FIRST_LAUNCH, true);

            editor.apply();
            //            Log.d(TAG, "key: " + getSharedPreferences(preferenceName, MODE_PRIVATE).getString(key, ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findPassword() {
        Bundle bundle = new Bundle();
        bundle.putInt("flag", AppConstant.VERTIFY_RESETPASSWORD);
        Intent intent = new Intent(LoginActivity.this, RegistPhoneActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, AppConstant.CODE_LOGIN_FINDPWD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //如果是从修改初始密码界面返回。
        if (requestCode == AppConstant.CODE_LOGIN_REGISTER && resultCode == Activity.RESULT_OK) {
            EventManager.ins().sendEvent(EventTag.ACCOUNT_LOGIN, 0, 0, null);

            Bundle bundle = data.getExtras();
            mHasEditFirstPassword = bundle.getBoolean("hasEditFirstPassword");

        } else if (requestCode == AppConstant.CODE_LOGIN_FINDPWD && resultCode == Activity.RESULT_OK) {
            //如果是从忘记密码界面返回。
        }
        //每次结束记得隐藏加载条。
        hideLoadingDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (etStudentNo != null) {
            SoftKeyboardUtil.hideSoftKeyboard(etStudentNo);
        }
        if (etPassword != null) {
            SoftKeyboardUtil.hideSoftKeyboard(etPassword);
        }
        hideLoadingDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkHttpUtils.getInstance().cancelTag(TAG);
    }
}
