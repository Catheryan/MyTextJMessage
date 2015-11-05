package cn.jpush.im.android.demo.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import cn.jpush.im.android.api.Conversation;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.content.ImageContent;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.event.ConversationRefreshEvent;
import cn.jpush.im.android.api.event.GroupMemberAddedEvent;
import cn.jpush.im.android.api.event.GroupMemberExitEvent;
import cn.jpush.im.android.api.event.GroupMemberRemovedEvent;
import cn.jpush.im.android.demo.adapter.MsgListAdapter;
import cn.jpush.im.android.demo.application.JPushDemoApplication;
import cn.jpush.im.android.demo.controller.ChatController;
import cn.jpush.im.android.demo.controller.RecordVoiceBtnController;
import cn.jpush.im.android.demo.receiver.MessageEventReceiver;
import cn.jpush.im.android.demo.view.ChatView;

import com.testdemo.R;

/*
 * 对话界面
 */
public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";

	private ChatView mChatView;
	private ChatController mChatController;
	public static String SIGN = "";
	private RefreshChatListReceiver mChatReceiver;
//	private Conversation mConv;
	private String mTargetID;
    public static Context mChatActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        JMessageClient.registerEventReceiver(this);
		setContentView(R.layout.activity_chat);
        mChatActivity = this;
		mChatView = (ChatView) findViewById(R.id.chat_view);
		mChatView.initModule();
		mChatController = new ChatController(mChatView, this);
		mChatView.setListeners(mChatController);
        mChatView.setOnTouchListener(mChatController);
		mChatView.setOnScrollListener(mChatController);
		initReceiver();
        mChatView.setToBottom();
	}

	// 更新消息的广播
	private void initReceiver() {
		// 注册更新消息列表的广播
		mChatReceiver = new RefreshChatListReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(JPushDemoApplication.REFRESH_CHATTING_ACTION);
		filter.addAction(JPushDemoApplication.REFRESH_CHATTING_ACTION_IMAGE);
        filter.addAction(JPushDemoApplication.ADD_GROUP_MEMBER_ACTION);
        filter.addAction(JPushDemoApplication.REMOVE_GROUP_MEMBER_ACTION);
        filter.addAction(JPushDemoApplication.UPDATE_GROUP_NAME_ACTION);
		registerReceiver(mChatReceiver, filter);
	}

	private class RefreshChatListReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent data) {
            if(data != null){
                final int messageID = data.getIntExtra("messageID", 0);
                mTargetID = data.getStringExtra("targetID");
                boolean isGroup = data.getBooleanExtra("isGroup", false);
//                if(isGroup)
//                    mConv = JMessageClient.getConversation(ConversationType.group, mTargetID);
//                else mConv = JMessageClient.getConversation(ConversationType.single, mTargetID);
                if (data.getAction().equals(
                        JPushDemoApplication.REFRESH_CHATTING_ACTION)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (messageID != 0)
                                mChatController.addMessage();
                        }
                    });
                } else if (data.getAction().equals(
                    JPushDemoApplication.REFRESH_CHATTING_ACTION_IMAGE)) {
                    handleImgRefresh(data, isGroup);
                }else if(data.getAction().equals(
                    JPushDemoApplication.UPDATE_GROUP_NAME_ACTION)){
                    mChatView.setChatTitle(data.getStringExtra("newGroupName"));
                }
            }
		}

	}

    @Override
    public void handleMsg(Message msg) {
        switch (msg.what) {
            case JPushDemoApplication.REFRESH_GROUP_NAME:
                if(mChatController.getConversation() != null)
                    mChatView.setChatTitle(mChatController.getConversation().getDisplayName());
                break;
            case JPushDemoApplication.ADD_GROUP_MEMBER_EVENT:
                long groupID = msg.getData().getLong("groupID");
                mChatController.addGroupMember(groupID);
                break;
            case JPushDemoApplication.REMOVE_GROUP_MEMBER_EVENT:
                groupID = msg.getData().getLong("groupID");
                boolean deleted = msg.getData().getBoolean("deleted", false);
                if(mChatController.isGroup() && mChatController.getGroupID() == groupID && deleted) {
                    mChatView.dismissRightBtn();
                }
                mChatController.removeGroupMember(groupID);
                break;
            case JPushDemoApplication.ON_GROUP_EXIT_EVENT:
                groupID = msg.getData().getLong("groupID");
                boolean isCreator = msg.getData().getBoolean("isCreator", false);
                mChatController.onGroupExit(groupID, isCreator);
                break;
        }
    }

    private void handleImgRefresh(Intent data, boolean isGroup) {
        mTargetID = data.getStringExtra("targetID");
        long groupID = data.getLongExtra("groupID", 0);
        Log.i(TAG, "Refresh Image groupID: " + groupID);
        //判断是否在当前会话中发图片
        if(mTargetID != null){
            if(mTargetID.equals(mChatController.getTargetID())){
                // 可能因为从其他界面回到聊天界面时，MsgListAdapter已经收到更新的消息了
                // 但是ListView没有刷新消息，要重新new Adapter, 并把这个Adapter传到ChatController
                // 保证ChatActivity和ChatController使用同一个Adapter
                mChatController.setAdapter(new MsgListAdapter(
                        ChatActivity.this, isGroup, mTargetID, groupID));
                // 重新绑定Adapter
                mChatView.setChatListAdapter(mChatController.getAdapter());
//                mChatController.refresh();
                mChatController.getAdapter().setSendImg(data.getIntArrayExtra("msgIDs"));
            }
        }else if(groupID != 0){
            if(groupID == mChatController.getGroupID()){
                mChatController.setAdapter(new MsgListAdapter(
                        ChatActivity.this, isGroup, mTargetID, groupID));
                // 重新绑定Adapter
                mChatView.setChatListAdapter(mChatController.getAdapter());
//                mChatController.refresh();
                mChatController.getAdapter().setSendImg(data.getIntArrayExtra("msgIDs"));
            }
        }



    }

    @Override
	public void onBackPressed() {
        if(RecordVoiceBtnController.mIsPressed){
            mChatView.dismissRecordDialog();
            mChatView.releaseRecorder();
            RecordVoiceBtnController.mIsPressed = false;
        }
        if(mChatController.mIsShowMoreMenu){
            Log.i(TAG, "onBackPressed");
            mChatView.dismissMoreMenu();
            mChatController.dismissSoftInput();
            ChatController.mIsShowMoreMenu = false;
        }else{
            if(mChatController.isGroup()){
                long groupID = mChatController.getGroupID();
                Log.i(TAG, "groupID "  + groupID);
                Conversation conv = JMessageClient.getConversation(ConversationType.group, groupID);
                conv.resetUnreadCount();
            }else{
                mTargetID = mChatController.getTargetID();
                Conversation conv = JMessageClient.getConversation(ConversationType.single, mTargetID);
                conv.resetUnreadCount();
            }

//            JMessageClient.exitConversaion();
            super.onBackPressed();
        }
	}

	/**
	 * 释放资源
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
        JMessageClient.unRegisterEventReceiver(this);
		super.onDestroy();
		unregisterReceiver(mChatReceiver);
		ChatActivity.SIGN = "";
		mChatController.releaseMediaPlayer();
        mChatView.releaseRecorder();
	}

	@Override
	protected void onPause() {
        Log.i(TAG, "onPause!");
        RecordVoiceBtnController.mIsPressed = false;
		super.onPause();
	}

    @Override
    protected void onStop(){
        if(mChatController.mIsShowMoreMenu){
            mChatView.dismissMoreMenu();
            mChatController.dismissSoftInput();
            ChatController.mIsShowMoreMenu = false;
        }
        ChatActivity.SIGN = "";
        if(mChatController.getConversation() != null)
            mChatController.getConversation().resetUnreadCount();
        super.onStop();
    }

	@Override
	protected void onResume() {
        if(!RecordVoiceBtnController.mIsPressed)
            mChatView.dismissRecordDialog();
        ChatActivity.SIGN = mChatController.getSign();
		NotificationManager manager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
        int cancelID = MessageEventReceiver.mNotificationList.indexOf(mChatController.getTargetID());
        if(cancelID != -1)
        manager.cancel(cancelID);
        mChatController.refresh();
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
		if (requestCode == JPushDemoApplication.REQUESTCODE_TAKE_PHOTO) {
            Conversation conv = mChatController.getConversation();
            try{
                String path = mChatController.getPhotoPath();
                File file = new File(path);
                ImageContent content = new ImageContent(file);
                cn.jpush.im.android.api.Message msg = conv.createSendMessage(content);
                boolean isGroup = getIntent().getBooleanExtra("isGroup", false);
                Intent intent = new Intent();
                intent.putExtra("msgIDs",new int[]{msg.getId()});
                if(conv.getType() == ConversationType.group) {
                    intent.putExtra("groupID", Long.parseLong(conv.getTargetId()));
                }else{
                    intent.putExtra("targetID",msg.getTargetID());
                }
                handleImgRefresh(intent,isGroup);
//                mChatController.refresh();
            }catch (FileNotFoundException e){
                Log.i(TAG, "create file failed!");
            }catch (NullPointerException e){
                Log.i(TAG, "onActivityResult unexpected result");
            }
            return;
			}
	}

	public void StartChatDetailActivity(boolean isGroup, String targetID, long groupID) {
        ChatActivity.SIGN = "";
		Intent intent = new Intent();
		intent.putExtra("isGroup", isGroup);
		intent.putExtra("targetID", targetID);
        intent.putExtra("groupID", groupID);
		intent.setClass(this, ChatDetailActivity.class);
		startActivity(intent);
	}

	public void StartPickPictureTotalActivity(Intent intent) {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, this.getString(R.string.sdcard_not_exist_toast), Toast.LENGTH_SHORT).show();
			return;
		} else {
            ChatActivity.SIGN = "";
			intent.setClass(this, PickPictureTotalActivity.class);
			startActivity(intent);
		}
	}

    public void onEvent(ConversationRefreshEvent conversationRefreshEvent){
        mHandler.sendEmptyMessage(JPushDemoApplication.REFRESH_GROUP_NAME);
    }

    public void onEvent(GroupMemberAddedEvent groupMemberAddedEvent){
        long groupID = groupMemberAddedEvent.getGroupID();
        List<String> members = groupMemberAddedEvent.getMembers();
        Log.i(TAG, "onGroupMemberAdded");
        for (String member : members) {
            Log.i(TAG, member + getString(R.string.join_group_toast));

            android.os.Message msg = mHandler.obtainMessage();
            msg.what = JPushDemoApplication.ADD_GROUP_MEMBER_EVENT;
            Bundle bundle = new Bundle();
            bundle.putLong("groupID", groupID);
            msg.setData(bundle);
            msg.sendToTarget();
        }
    }

    public void onEvent(GroupMemberRemovedEvent groupMemberRemovedEvent){
        long groupID = groupMemberRemovedEvent.getGroupID();
        List<String> members = groupMemberRemovedEvent.getMembers();
        Log.i(TAG, "onGroupMemberRemoved");
        for (String member : members) {
            android.os.Message msg = mHandler.obtainMessage();
            msg.what = JPushDemoApplication.REMOVE_GROUP_MEMBER_EVENT;
            Bundle bundle = new Bundle();
            if(member.equals(JMessageClient.getMyInfo().getUserName())){
                bundle.putBoolean("deleted", true);
            }

            bundle.putLong("groupID", groupID);
            msg.setData(bundle);
            msg.sendToTarget();
        }
    }

    public void onEvent(GroupMemberExitEvent groupMemberExitEvent){
        long groupID = groupMemberExitEvent.getGroupID();
        boolean isCreator = groupMemberExitEvent.containsGroupOwner();
        List<String> members = groupMemberExitEvent.getMembers();
            if(isCreator){
                android.os.Message msg = mHandler.obtainMessage();
                msg.what = JPushDemoApplication.ON_GROUP_EXIT_EVENT;
                Bundle bundle = new Bundle();
                bundle.putBoolean("isCreator", true);
                bundle.putLong("groupID", groupID);
                msg.setData(bundle);
                msg.sendToTarget();
            }else {
                for(String userName : members){
                    android.os.Message msg = mHandler.obtainMessage();
                    msg.what = JPushDemoApplication.ON_GROUP_EXIT_EVENT;
                    Bundle bundle = new Bundle();
                    Log.i(TAG, userName + getString(R.string.exit_group_event));
                    bundle.putLong("groupID", groupID);
                    msg.setData(bundle);
                    msg.sendToTarget();
                }

            }
    }

}
