package com.xgr.wonderful.entity;

import cn.bmob.v3.BmobObject;


/**
 * @author kingofglory
 *         email: kingofglory@yeah.net
 *         blog:  http:www.google.com
 * @date 2014-4-2
 * TODO
 */

public class Comment extends BmobObject{
	
	public static final String TAG = "Comment";

	private BUser user;
	private String commentContent;
	public BUser getUser() {
		return user;
	}
	public void setUser(BUser user) {
		this.user = user;
	}
	public String getCommentContent() {
		return commentContent;
	}
	public void setCommentContent(String commentContent) {
		this.commentContent = commentContent;
	}
	
	

}
