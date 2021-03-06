package cn.jpush.im.android.demo.receiver;


import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import cn.jpush.im.android.api.Conversation;
import cn.jpush.im.android.api.JMessageClient;
import cn.jpush.im.android.api.Message;
import cn.jpush.im.android.api.content.TextContent;
import cn.jpush.im.android.api.enums.ConversationType;
import cn.jpush.im.android.api.event.MessageEvent;
import cn.jpush.im.android.demo.activity.ChatActivity;
import cn.jpush.im.android.demo.application.JPushDemoApplication;

import com.testdemo.R;

public class MessageEventReceiver {

    private static final String TAG = MessageEventReceiver.class.getSimpleName();

    private Context context;
    private static int count = 0;
    public static List<String> mNotificationList = new ArrayList<String>();
    private static long mLastTime = 0;

    public MessageEventReceiver(Context context) {
        this.context = context;
        JMessageClient.registerEventReceiver(this);
    }

    public void onEvent(MessageEvent event) {
        ConversationType convType = event.getConversationType();
        String targetID = event.getTargetID();
        int messageID = event.getMsgID();
        boolean mIsGroup;
        if (messageID != 0) {
            if (convType == ConversationType.group) {
                mIsGroup = true;
            } else {
                mIsGroup = false;
            }
            Conversation conv = JMessageClient.getConversation(convType, targetID);
            Log.d(TAG, "conv.toString() " + conv.toString());
            Message msg = conv.getMessage(messageID);
            Log.i(TAG, "msg = " + msg.toString());
            String content;
            switch (msg.getContentType()) {
                case image:
                    content = context.getString(R.string.noti_content_type_img);
                    break;
                case voice:
                    content = context.getString(R.string.noti_content_type_voice);
                    break;
                case location:
                    content = context.getString(R.string.noti_content_type_location);
                    break;
                default:
                    content = ((TextContent) msg.getContent()).getText();

            }
            if (ChatActivity.SIGN.equals(conv.getType() + ":"
                    + conv.getTargetId())) {
                Intent intent = new Intent(JPushDemoApplication.REFRESH_CHATTING_ACTION);
                intent.putExtra("messageID", messageID);
                intent.putExtra("isGroup", mIsGroup);
                intent.putExtra("targetID", conv.getTargetId());
                context.sendBroadcast(intent);
            }
            else {
                Intent intent = new Intent(JPushDemoApplication.REFRESH_CONVLIST_ACTION);
                intent.putExtra("targetID", conv.getTargetId());
                Log.i("Receiver", "receive conversationlist action!");
                context.sendBroadcast(intent);
                NotificationManager manager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                Notification notification = new Notification(R.drawable.icon, context.getText(R.string.noti_new_message_receive), System.currentTimeMillis());
                long currentTime = System.currentTimeMillis();
                if (currentTime - mLastTime > 5000) {
                    notification.defaults = Notification.DEFAULT_ALL;
                    mLastTime = System.currentTimeMillis();
                }
                notification.flags = Notification.FLAG_AUTO_CANCEL;
                Intent notificationIntent = new Intent(context, ChatActivity.class);
                notificationIntent.setAction(Intent.ACTION_MAIN);
//                    notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                notificationIntent.putExtra("targetID", conv.getTargetId());
                notificationIntent.putExtra("isGroup", mIsGroup);
                notificationIntent.putExtra("fromGroup", false);
                if (!mNotificationList.contains(conv.getTargetId())) {
                    if (count < 5) {
                        if (mNotificationList.size() < 5) {
                            mNotificationList.add(conv.getTargetId());
                            count++;
                        } else {
                            mNotificationList.set(count, conv.getTargetId());
                            count++;
                        }
                    } else if (count >= 5) {
                        count = 0;
                        mNotificationList.set(count, conv.getTargetId());
                        count++;
                    }
                }
                notificationIntent.putExtra("count", mNotificationList.indexOf(conv.getTargetId()));
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent contentIntent = PendingIntent.getActivity(context, mNotificationList.indexOf(conv.getTargetId()), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setLatestEventInfo(context, conv.getDisplayName(), content, contentIntent);
                manager.notify(mNotificationList.indexOf(conv.getTargetId()), notification);
            }
        }

    }
}
