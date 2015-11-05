package com.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.testdemo.R;

/**
 * 这是自己创建的Activity，在这里可以跳转到LoginActivity(JMessage自带)
 * @author Administrator
 *
 * 除了按照官方文档描述的操作，还应该知道项目名称不是包名称；
 * 	注册一个MyResiver，既能实现JPush推送功能
 * 	点击Natifacation事件在MyReciver中捕捉
 * 	
 */
public class MainActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		Button btn = (Button) findViewById(R.id.btn);
		btn.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this , cn.jpush.im.android.demo.activity.MainActivity.class));
			}
		});
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

}
