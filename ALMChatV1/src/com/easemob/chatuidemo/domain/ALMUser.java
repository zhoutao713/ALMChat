package com.easemob.chatuidemo.domain;

import cn.bmob.v3.BmobUser;

import com.easemob.chat.EMContact;

public class ALMUser {
	private EMContact emUser;
	private BmobUser bmobUser;
	public EMContact getEmUser() {
		return emUser;
	}
	public void setEmUser(EMContact emUser) {
		this.emUser = emUser;
	}
	public BmobUser getBmobUser() {
		return bmobUser;
	}
	public void setBmobUser(BmobUser bmobUser) {
		this.bmobUser = bmobUser;
	}
	private static  ALMUser almuser = new ALMUser();
	public static ALMUser instance(){
		return almuser;
	}
	
	
}
