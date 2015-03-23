/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.easemob.chatuidemo.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import cn.bmob.im.BmobUserManager;
import cn.bmob.v3.BmobInstallation;
import cn.bmob.v3.listener.SaveListener;

import com.easemob.EMCallBack;
import com.easemob.chat.EMChatManager;
import com.easemob.chat.EMContactManager;
import com.easemob.chat.EMGroupManager;
import com.easemob.chatuidemo.ALMApplication;
import com.easemob.chatuidemo.Constant;
import com.easemob.chatuidemo.DemoHXSDKHelper;
import com.easemob.chatuidemo.R;
import com.easemob.chatuidemo.db.UserDao;
import com.easemob.chatuidemo.domain.EUser;
import com.easemob.chatuidemo.utils.CommonUtils;
import com.easemob.exceptions.EaseMobException;
import com.easemob.util.EMLog;
import com.easemob.util.HanziToPinyin;
import com.umeng.analytics.MobclickAgent;
import com.xgr.wonderful.entity.BUser;

/**
 * 登陆页面
 * 
 */
public class LoginActivity extends BaseActivity {
	public static final int REQUEST_CODE_SETNICK = 1;
	private EditText usernameEditText;
	private EditText passwordEditText;

	private boolean progressShow;
	private boolean autoLogin = false;
	
	private String currentUsername;
	private String currentPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 如果用户名密码都有，直接进入主页面
		if (DemoHXSDKHelper.getInstance().isLogined()) {
			autoLogin = true;
			startActivity(new Intent(LoginActivity.this, MainActivity.class));

			return;
		}
		setContentView(R.layout.activity_login);

		usernameEditText = (EditText) findViewById(R.id.username);
		passwordEditText = (EditText) findViewById(R.id.password);

		// 如果用户名改变，清空密码
		usernameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				passwordEditText.setText(null);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});
		if (ALMApplication.getInstance().getUserName() != null) {
			usernameEditText.setText(ALMApplication.getInstance().getUserName());
		}
	}

	/**
	 * 登陆
	 * 
	 * @param view
	 */
	public void login(View view) {
		if (!CommonUtils.isNetWorkConnected(this)) {
			Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
			return;
		}
		currentUsername = usernameEditText.getText().toString().trim();
		currentPassword = passwordEditText.getText().toString().trim();
		
		if(TextUtils.isEmpty(currentUsername)){
			Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
			return;
		}
		if(TextUtils.isEmpty(currentPassword)){
			Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = new Intent(LoginActivity.this, com.easemob.chatuidemo.activity.AlertDialog.class);
		intent.putExtra("editTextShow", true);
		intent.putExtra("titleIsCancel", true);
		intent.putExtra("msg", getResources().getString(R.string.please_set_the_current));
		intent.putExtra("edit_text", currentUsername);
		startActivityForResult(intent, REQUEST_CODE_SETNICK);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CODE_SETNICK) {
				ALMApplication.currentUserNick = data.getStringExtra("edittext");

				progressShow = true;
				final ProgressDialog pd = new ProgressDialog(LoginActivity.this);
				pd.setCanceledOnTouchOutside(false);
				pd.setOnCancelListener(new OnCancelListener() {

					@Override
					public void onCancel(DialogInterface dialog) {
						progressShow = false;
					}
				});
				pd.setMessage(getString(R.string.Is_landing));
				pd.show();

				final long start = System.currentTimeMillis();
				// 调用easemob sdk登陆方法登陆聊天服务器
				EMChatManager.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

					@Override
					public void onSuccess() {
						//umeng自定义事件，开发者可以把这个删掉
						loginSuccess2Umeng(start);
						//线程调用bmob的登陆方法
						if (!progressShow) {
							return;
						}
						userManager.login(currentUsername, currentPassword,new SaveListener() {
							
							@Override
							public void onSuccess() {
								onsuccessLogin(pd);
							}
							@Override
							public void onFailure(int i, String s) {
//								onFailLogin(pd,s);
								bmobRegister(currentUsername,currentPassword,pd);
							}
						});
						
						
						
					}

					@Override
					public void onProgress(int progress, String status) {
					}

					@Override
					public void onError(final int code, final String message) {
						loginFailure2Umeng(start,code,message);
						if (!progressShow) {
							return;
						}
						onFailLogin(pd,message);
					}
				});

			}
		}
	}
	private void onFailLogin(final ProgressDialog pd,final String message){
		runOnUiThread(new Runnable() {
			public void run() {
				pd.dismiss();
				Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	private void onsuccessLogin(final ProgressDialog pd){
		// 登陆成功，保存用户名密码
		ALMApplication.getInstance().setUserName(currentUsername);
		ALMApplication.getInstance().setPassword(currentPassword);
		runOnUiThread(new Runnable() {
			public void run() {
				pd.setMessage(getString(R.string.list_is_for));
			}
		});
		try {
			// ** 第一次登录或者之前logout后再登录，加载所有本地群和回话
			// ** manually load all local groups and
			// conversations in case we are auto login
			EMGroupManager.getInstance().loadAllGroups();
			EMChatManager.getInstance().loadAllConversations();
			//处理好友和群组
			processContactsAndGroups();
		} catch (Exception e) {
			e.printStackTrace();
			//取好友或者群聊失败，不让进入主页面
			runOnUiThread(new Runnable() {
                public void run() {
                    pd.dismiss();
                    ALMApplication.getInstance().logout(null);
                    Toast.makeText(getApplicationContext(), R.string.login_failure_failed, 1).show();
                }
            });
			return;
		}
		//更新当前用户的nickname 此方法的作用是在ios离线推送时能够显示用户nick
		boolean updatenick = EMChatManager.getInstance().updateCurrentUserNick(ALMApplication.currentUserNick.trim());
		if (!updatenick) {
			Log.e("LoginActivity", "update current user nick fail");
		}
		if (!LoginActivity.this.isFinishing())
			pd.dismiss();
		// 进入主页面
		startActivity(new Intent(LoginActivity.this, MainActivity.class));
		finish();
		
	}

	 private void processContactsAndGroups() throws EaseMobException {
         // demo中简单的处理成每次登陆都去获取好友username，开发者自己根据情况而定
         List<String> usernames = EMContactManager.getInstance().getContactUserNames();
         System.out.println("----------------"+usernames.toString());
         EMLog.d("roster", "contacts size: " + usernames.size());
         Map<String, EUser> userlist = new HashMap<String, EUser>();
         for (String username : usernames) {
             EUser user = new EUser();
             user.setUsername(username);
             setUserHearder(username, user);
             userlist.put(username, user);
         }
         // 添加user"申请与通知"
         EUser newFriends = new EUser();
         newFriends.setUsername(Constant.NEW_FRIENDS_USERNAME);
         String strChat = getResources().getString(R.string.Application_and_notify);
         newFriends.setNick(strChat);
         
         userlist.put(Constant.NEW_FRIENDS_USERNAME, newFriends);
         // 添加"群聊"
         EUser groupUser = new EUser();
         String strGroup = getResources().getString(R.string.group_chat);
         groupUser.setUsername(Constant.GROUP_USERNAME);
         groupUser.setNick(strGroup);
         groupUser.setHeader("");
         userlist.put(Constant.GROUP_USERNAME, groupUser);

         // 存入内存
         ALMApplication.getInstance().setContactList(userlist);
         // 存入db
         UserDao dao = new UserDao(LoginActivity.this);
         List<EUser> users = new ArrayList<EUser>(userlist.values());
         dao.saveContactList(users);
         
         //获取黑名单列表
         List<String> blackList = EMContactManager.getInstance().getBlackListUsernamesFromServer();
         //保存黑名单
         EMContactManager.getInstance().saveBlackList(blackList);

         // 获取群聊列表(群聊里只有groupid和groupname等简单信息，不包含members),sdk会把群组存入到内存和db中
         EMGroupManager.getInstance().getGroupsFromServer();
     }
	
	/**
	 * 注册
	 * 
	 * @param view
	 */
	public void register(View view) {
		startActivityForResult(new Intent(this, RegisterActivity.class), 0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (autoLogin) {
			return;
		}
	}

	/**
	 * 设置hearder属性，方便通讯中对联系人按header分类显示，以及通过右侧ABCD...字母栏快速定位联系人
	 * 
	 * @param username
	 * @param user
	 */
	protected void setUserHearder(String username, EUser user) {
		String headerName = null;
		if (!TextUtils.isEmpty(user.getNick())) {
			headerName = user.getNick();
		} else {
			headerName = user.getUsername();
		}
		if (username.equals(Constant.NEW_FRIENDS_USERNAME)) {
			user.setHeader("");
		} else if (Character.isDigit(headerName.charAt(0))) {
			user.setHeader("#");
		} else {
			user.setHeader(HanziToPinyin.getInstance().get(headerName.substring(0, 1)).get(0).target.substring(0, 1).toUpperCase());
			char header = user.getHeader().toLowerCase().charAt(0);
			if (header < 'a' || header > 'z') {
				user.setHeader("#");
			}
		}
	}
	
	private void loginSuccess2Umeng(final long start) {
		runOnUiThread(new Runnable() {
			public void run() {
				long costTime = System.currentTimeMillis() - start;
				Map<String, String> params = new HashMap<String, String>();
				params.put("status", "success");
				MobclickAgent.onEventValue(LoginActivity.this, "login1", params, (int) costTime);
				MobclickAgent.onEventDuration(LoginActivity.this, "login1", (int) costTime);
			}
		});
	}
	private void loginFailure2Umeng(final long start, final int code, final String message) {
		runOnUiThread(new Runnable() {
			public void run() {
				long costTime = System.currentTimeMillis() - start;
				Map<String, String> params = new HashMap<String, String>();
				params.put("status", "failure");
				params.put("error_code", code + "");
				params.put("error_description", message);
				MobclickAgent.onEventValue(LoginActivity.this, "login1", params, (int) costTime);
				MobclickAgent.onEventDuration(LoginActivity.this, "login1", (int) costTime);
				
			}
		});
	}
	
	
	private void bmobRegister(String name,String password,final ProgressDialog pd){
		//由于每个应用的注册所需的资料都不一样，故IM sdk未提供注册方法，用户可按照bmod SDK的注册方式进行注册。
		//注册的时候需要注意两点：1、User表中绑定设备id和type，2、设备表中绑定username字段
		final BUser bu = new BUser();
		bu.setUsername(name);
		bu.setPassword(password);
		//将user和设备id进行绑定
		bu.setDeviceType("android");
		bu.setInstallId(BmobInstallation.getInstallationId(this));
		bu.signUp(LoginActivity.this, new SaveListener() {

			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
		/*		pd.dismiss();
				ShowToast("注册成功");
				// 将设备与username进行绑定
				userManager.bindInstallationForRegister(bu.getUsername());
				//更新地理位置信息
				updateUserLocation();
				//发广播通知登陆页面退出
				sendBroadcast(new Intent(BmobConstants.ACTION_REGISTER_SUCCESS_FINISH));
				// 启动主页
				Intent intent = new Intent(RegisterActivity.this,MainActivity.class);
				startActivity(intent);
				finish();*/
				onsuccessLogin(pd);
			}

			@Override
			public void onFailure(int arg0, String arg1) {
				
				onFailLogin(pd,arg1);
				// TODO Auto-generated method stub
				/*BmobLog.i(arg1);
				ShowToast("注册失败:" + arg1);
				progress.dismiss();*/
			}
		});
		
		
	}
}
