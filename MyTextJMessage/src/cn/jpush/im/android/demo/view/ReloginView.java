package cn.jpush.im.android.demo.view;



import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.testdemo.R;

public class ReloginView extends LinearLayout{

	private EditText mPassword;
	private Button mReloginBtn;
	private TextView mSwitchBtn;
    private TextView mUserNameTv;
    private Button mRegisterBtn;
	
	public ReloginView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public void initModule(){
		mPassword = (EditText) findViewById(R.id.relogin_password);
		mReloginBtn = (Button) findViewById(R.id.relogin_btn);
		mSwitchBtn = (TextView) findViewById(R.id.relogin_switch_user_btn);
        mUserNameTv = (TextView) findViewById(R.id.username_tv);
        mRegisterBtn = (Button) findViewById(R.id.register_btn);
	}
	
	public void setListeners(OnClickListener onClickListener){
		mReloginBtn.setOnClickListener(onClickListener);
		mSwitchBtn.setOnClickListener(onClickListener);
        mRegisterBtn.setOnClickListener(onClickListener);
	}

	public String getPassword() {
		return mPassword.getText().toString().trim();
	}
	
	public void passwordError(Context context) {
		Toast.makeText(context, context.getString(R.string.password_not_null_toast), Toast.LENGTH_SHORT).show();
	}

    public void setUserName(String userName) {
        mUserNameTv.setText(userName);
    }
}
