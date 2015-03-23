package com.easemob.chatuidemo.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.listener.FindListener;

import com.easemob.chatuidemo.ALMApplication;
import com.easemob.chatuidemo.R;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.xgr.wonderful.adapter.AIContentAdapter;
import com.xgr.wonderful.db.DatabaseUtil;
import com.xgr.wonderful.entity.QiangYu;
import com.xgr.wonderful.ui.CommentActivity;
import com.xgr.wonderful.ui.RegisterAndLoginActivity;
import com.xgr.wonderful.util.ActivityUtil;
import com.xgr.wonderful.util.Constant;
import com.xgr.wonderful.util.LogUtils;

public class WonderfulFragment extends Fragment{

	private ImageView rightMenu;
	private ProgressBar progressbar;
	public static String TAG;
	protected Context mContext;
	private int pageNum;
	private AIContentAdapter mAdapter;
	private ArrayList<QiangYu> mListItems;
	private ListView actualListView;
	private PullToRefreshListView mPullRefreshListView;
	private View contentView ;
	private TextView networkTips;
	private boolean pullFromUser;
	private String lastItemTime;//当前列表结尾的条目的创建时间，
	
	private RefreshType mRefreshType = RefreshType.LOAD_MORE;
	public enum RefreshType{
		REFRESH,LOAD_MORE
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Bmob.initialize(getActivity(),Constant.BMOB_APP_ID);
		TAG = this.getClass().getSimpleName();
		mContext = getActivity();
		pageNum = 0;
		
	}
	private String getCurrentTime(){
		 SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	     String times = formatter.format(new Date(System.currentTimeMillis()));
	     return times;
	}
	OnClickListener rightClick = new OnClickListener(){

		@Override
		public void onClick(View v) {

	        // TODO Auto-generated method stub
	        switch(v.getId()) {
	            case R.id.topbar_menu_right:
	              //当前用户登录
	                BmobUser currentUser = BmobUser.getCurrentUser(getActivity());
	                if (null!=currentUser ) {
	                    // 允许用户使用应用,即有了用户的唯一标识符，可以作为发布内容的字段
	                    String name = currentUser.getUsername();
	                    String email = currentUser.getEmail();
	                    LogUtils.i(TAG,"username:"+name+",email:"+email);
	                    Intent intent = new Intent();
	                    intent.setClass(getActivity(), com.xgr.wonderful.ui.EditActivity.class);
	                    startActivity(intent);
	                } else {
	                    // 缓存用户对象为空时， 可打开用户注册界面…
	                    Toast.makeText(getActivity(), "请先登录。",
	                            Toast.LENGTH_SHORT).show();
//	                  redictToActivity(mContext, RegisterAndLoginActivity.class, null);
	                    Intent intent = new Intent();
	                    intent.setClass(getActivity(), LoginActivity.class);
	                    startActivity(intent);
	                }
	                break;
	            default:
	                break;
	        }
	    
			
		}
		
	};


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		// inflater.inflate(R.layout.center_frame, container,false);
		
		contentView = inflater.inflate(R.layout.fragment_wonder, container,false);
		progressbar = (ProgressBar)contentView.findViewById(R.id.progressBar);
		mPullRefreshListView = (PullToRefreshListView)contentView
				.findViewById(R.id.pull_refresh_list);
		networkTips = (TextView)contentView.findViewById(R.id.networkTips);
		actualListView = mPullRefreshListView.getRefreshableView();
		mListItems = new ArrayList<QiangYu>();
		rightMenu = (ImageView)contentView.findViewById(R.id.topbar_menu_right);
		rightMenu.setOnClickListener(rightClick);
		mAdapter = new AIContentAdapter(mContext, mListItems);
		actualListView.setAdapter(mAdapter);
		if(mListItems.size() == 0){
			fetchData();
		}
		mPullRefreshListView.setState(State.RELEASE_TO_REFRESH, true);
		actualListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				ALMApplication.getInstance().setCurrentQiangYu(mListItems.get(position-1));
				Intent intent = new Intent();
				intent.setClass(getActivity(), CommentActivity.class);
				intent.putExtra("data", mListItems.get(position-1));
				startActivity(intent);
			}
		});
		mPullRefreshListView.setMode(Mode.BOTH);
		mPullRefreshListView.setOnRefreshListener(new OnRefreshListener2<ListView>() {

			@Override
			public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
				// TODO Auto-generated method stub
				String label = DateUtils.formatDateTime(getActivity(), System.currentTimeMillis(),
						DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
				refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
				mPullRefreshListView.setMode(Mode.BOTH);
				pullFromUser = true;
				mRefreshType = RefreshType.REFRESH;
				pageNum = 0;
				lastItemTime = getCurrentTime();
				fetchData();
			}

			@Override
			public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {
				// TODO Auto-generated method stub
				mRefreshType = RefreshType.LOAD_MORE;
				fetchData();
			}
		});
		mPullRefreshListView.setOnLastItemVisibleListener(new OnLastItemVisibleListener() {

			@Override
			public void onLastItemVisible() {
				// TODO Auto-generated method stub
				
			}
		});
		return contentView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		/*rightMenu = (ImageView)getView().findViewById(R.id.topbar_menu_right);
		rightMenu.setOnClickListener(onclickLinstener);
		progressbar = (ProgressBar)getView().findViewById(R.id.progressBar);
		mListItems = new ArrayList<QiangYu>();
		mAdapter = new AIContentAdapter(mContext, mListItems);
		actualListView.setAdapter(mAdapter);
		if(mListItems.size() == 0){
			fetchData();
		}*/
		
		
	}
	
	private OnClickListener onclickLinstener =new OnClickListener(){
	    @Override
	    public void onClick(View v) {
	        // TODO Auto-generated method stub
	        switch(v.getId()) {
	            
	            /*case R.id.topbar_menu_right:
	              //当前用户登录
	                BmobUser currentUser = BmobUser.getCurrentUser(MainActivity.this);
	                if (currentUser != null) {
	                    // 允许用户使用应用,即有了用户的唯一标识符，可以作为发布内容的字段
	                    String name = currentUser.getUsername();
	                    String email = currentUser.getEmail();
	                    LogUtils.i(TAG,"username:"+name+",email:"+email);
	                    Intent intent = new Intent();
	                    intent.setClass(getActivity(), EditActivity.class);
	                    startActivity(intent);
	                } else {
	                    // 缓存用户对象为空时， 可打开用户注册界面…
	                    Toast.makeText(MainActivity.this, "请先登录。",
	                            Toast.LENGTH_SHORT).show();
//	                  redictToActivity(mContext, RegisterAndLoginActivity.class, null);
	                    Intent intent = new Intent();
	                    intent.setClass(MainActivity.this, RegisterAndLoginActivity.class);
	                    startActivity(intent);
	                }
	                break;
	            default:
	                break;*/
	        	case R.id.topbar_menu_right:
	        	  Intent intent = new Intent();
	        	  intent.setClass(getActivity(), EditActivity.class);
	        	  startActivity(intent);
	        }
	    }
		
	};
	
	
	private static final int LOADING = 1;
	private static final int LOADING_COMPLETED = 2;
	private static final int LOADING_FAILED =3;
	private static final int NORMAL = 4;
	public void setState(int state){
		switch (state) {
		case LOADING:
			if(mListItems.size() == 0){
				mPullRefreshListView.setVisibility(View.GONE);
				progressbar.setVisibility(View.VISIBLE);
			}
			networkTips.setVisibility(View.GONE);
			
			break;
		case LOADING_COMPLETED:
			networkTips.setVisibility(View.GONE);
			progressbar.setVisibility(View.GONE);
			
		    mPullRefreshListView.setVisibility(View.VISIBLE);
		    mPullRefreshListView.setMode(Mode.BOTH);

			
			break;
		case LOADING_FAILED:
			if(mListItems.size()==0){
				mPullRefreshListView.setVisibility(View.VISIBLE);
				mPullRefreshListView.setMode(Mode.PULL_FROM_START);
				networkTips.setVisibility(View.VISIBLE);
			}
			progressbar.setVisibility(View.GONE);
			break;
		case NORMAL:
			
			break;
		default:
			break;
		}
	}
	public void fetchData(){
		setState(LOADING);
		BmobQuery<QiangYu> query = new BmobQuery<QiangYu>();
		query.order("-createdAt");
//		query.setCachePolicy(CachePolicy.NETWORK_ONLY);
		query.setLimit(Constant.NUMBERS_PER_PAGE);
		BmobDate date = new BmobDate(new Date(System.currentTimeMillis()));
		query.addWhereLessThan("createdAt", date);
		LogUtils.i(TAG,"SIZE:"+Constant.NUMBERS_PER_PAGE*pageNum);
		query.setSkip(Constant.NUMBERS_PER_PAGE*(pageNum++));
		LogUtils.i(TAG,"SIZE:"+Constant.NUMBERS_PER_PAGE*pageNum);
		query.include("author");
		query.findObjects(getActivity(), new FindListener<QiangYu>() {
			
			@Override
			public void onSuccess(List<QiangYu> list) {
				// TODO Auto-generated method stub
				LogUtils.i(TAG,"find success."+list.size());
				if(list.size()!=0&&list.get(list.size()-1)!=null){
					if(mRefreshType==RefreshType.REFRESH){
						mListItems.clear();
					}
					if(list.size()<Constant.NUMBERS_PER_PAGE){
						LogUtils.i(TAG,"已加载完所有数据~");
					}
					if(ALMApplication.getInstance().getCurrentUser().getBmobUser()!=null){
						list = DatabaseUtil.getInstance(mContext).setFav(list);
					}
					mListItems.addAll(list);
					mAdapter.notifyDataSetChanged();
					
					setState(LOADING_COMPLETED);
					mPullRefreshListView.onRefreshComplete();
				}else{
					ActivityUtil.show(getActivity(), "暂无更多数据~");
					pageNum--;
					setState(LOADING_COMPLETED);
					mPullRefreshListView.onRefreshComplete();
				}
			}

			@Override
			public void onError(int arg0, String arg1) {
				// TODO Auto-generated method stub
				LogUtils.i(TAG,"find failed."+arg1);
				pageNum--;
				setState(LOADING_FAILED);
				mPullRefreshListView.onRefreshComplete();
			}
		});
	}

	
}
