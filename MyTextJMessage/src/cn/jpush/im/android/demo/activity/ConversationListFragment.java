package cn.jpush.im.android.demo.activity;


import java.io.File;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import cn.jpush.im.android.api.Conversation;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.event.ConversationRefreshEvent;
import cn.jpush.im.android.demo.application.JPushDemoApplication;
import cn.jpush.im.android.demo.controller.ConversationListController;
import cn.jpush.im.android.demo.controller.MenuItemController;
import cn.jpush.im.android.demo.tools.NativeImageLoader;
import cn.jpush.im.android.demo.view.ConversationListView;
import cn.jpush.im.android.demo.view.MenuItemView;

import com.testdemo.R;

/*
 * 会话列表界面
 */
public class ConversationListFragment extends Fragment{
	private View mRootView;
	private ConversationListView mConvListView;
	private ConversationListController mConvListController;
	private MsgRefreshReceiver mMsgRefreshReceiver;
    private PopupWindow mMenuPopWindow;
    private View mMenuView;
    private MenuItemView mMenuItemView;
    private MenuItemController mMenuController;

	
	@Override
	public void onAttach(Activity activity) {
		//注册更新会话列表的广播
		mMsgRefreshReceiver = new MsgRefreshReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(JPushDemoApplication.REFRESH_CONVLIST_ACTION);
		getActivity().registerReceiver(mMsgRefreshReceiver, intentFilter);
		Log.i("MainActivity", "广播注册完毕");
		super.onAttach(activity);
	}


	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		JMessageClient.registerEventReceiver(this);
		LayoutInflater layoutInflater = getActivity().getLayoutInflater();
		mRootView = layoutInflater.inflate(R.layout.fragment_conv_list,
				(ViewGroup) getActivity().findViewById(R.id.main_view),
				false);
		mConvListView = new ConversationListView(mRootView, this.getActivity());
		mConvListView.initModule();
        mMenuView = getActivity().getLayoutInflater().inflate(R.layout.drop_down_menu, null);
		mConvListController = new ConversationListController(mConvListView, this);
		mConvListView.setListener(mConvListController);
		mConvListView.setItemListeners(mConvListController);
		mConvListView.setLongClickListener(mConvListController);
        mMenuPopWindow = new PopupWindow(mMenuView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
        mMenuItemView = new MenuItemView(mMenuView);
        mMenuItemView.initModule();
        mMenuController = new MenuItemController(mMenuItemView, this, mConvListController);
        mMenuItemView.setListeners(mMenuController);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);

	}

    public void showMenuPopWindow() {
        mMenuPopWindow.setTouchable(true);
        mMenuPopWindow.setOutsideTouchable(true);
        mMenuPopWindow.setBackgroundDrawable(new BitmapDrawable(getResources(),
                (Bitmap) null));
        if(mMenuPopWindow.isShowing()){
            mMenuPopWindow.dismiss();
        }else mMenuPopWindow.showAsDropDown(mRootView.findViewById(R.id.create_group_btn));
    }

    //接收会话列表更新的广播
	private class MsgRefreshReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(JPushDemoApplication.REFRESH_CONVLIST_ACTION)){
				Log.i("Receiver", "正在更新会话列表");
                String targetID = intent.getStringExtra("targetID");
                Conversation conv = JMessageClient.getConversation(ConversationType.single, targetID);
                if(conv != null){
                    //如果缓存了头像，直接刷新会话列表
                    if(NativeImageLoader.getInstance().getBitmapFromMemCache(targetID) != null){
                        mConvListController.refreshConvList();
                        //没有头像，从Conversation拿
                    }else {
                        File file = conv.getAvatar();
                        //拿到后缓存并刷新
                        if(file != null){
                            mConvListController.loadAvatarAndRefresh(targetID, file.getAbsolutePath());
                            //conversation中没有头像，直接刷新
                        }else mConvListController.refreshConvList();
                    }
                }else {
                    mConvListController.refreshConvList();
                }
			}
		}
	}

	public void onEvent(ConversationRefreshEvent conversationRefreshEvent){
		mConvListController.getAdapter().notifyDataSetChanged();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		ViewGroup p = (ViewGroup) mRootView.getParent();
		if (p != null) {
			p.removeAllViewsInLayout();
		}
		return mRootView;
	}

	@Override
	public void onResume() {

        if(JMessageClient.getMyInfo() == null || TextUtils.isEmpty(JMessageClient.getMyInfo().getUserName())){
            Intent intent = new Intent();
            intent.setClass(this.getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        }else {
            dismissPopWindow();
            mConvListController.refreshConvList();
        }
		super.onResume();
	}

    public void dismissPopWindow(){
        if(mMenuPopWindow.isShowing()){
            mMenuPopWindow.dismiss();
        }
    }


	@Override
	public void onDestroy() {
		getActivity().unregisterReceiver(mMsgRefreshReceiver);
		JMessageClient.unRegisterEventReceiver(this);
		super.onDestroy();
	}



	public void StartCreateGroupActivity() {
		Intent intent = new Intent();
		intent.setClass(getActivity(), CreateGroupActivity.class);
		startActivity(intent);
	}


}
