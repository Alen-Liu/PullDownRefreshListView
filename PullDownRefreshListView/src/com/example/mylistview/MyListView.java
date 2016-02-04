package com.example.mylistview;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
public class MyListView extends ListView implements OnScrollListener{

	/**
	 * 下拉刷新 listview  Head 和 下拉动画
	 * 1. 继承自 OnScrollListener 并且 重写 onTouchEvent 函数
	 * 2. 定义不同的状态,在onTouchEvent的时候进行处理.
	 * 3. 耗时的更新数据的操作 需要使用异步线程进行处理.
	 * 4. 通过setPaddingTop 或者 layout的方式来处理HeadView的位置和状态变化.
	 * */
	View mHeadView;
	int mHeadViewHeight;//Head View的高度
	Context mContext;
	
	int mCurrentScrollState;//当前的Scroll 状态
	int mFristVisblePos;
	private final static int PULL_NORMAL = 0;   //正常状态
	private final static int PULL_DOWN = 1;  //进入下拉刷新状态
	private final static int PULL_STOP = 2;   //进入松手刷新状态
	private final static int PULL_LOAD = 3;   //松手后反弹后加载状态
	private final static int PULL_BACK = 3;   //松手后反弹后加载状态
	int mPullState = PULL_NORMAL;//当前的pull-refresh 状态 默认

	private int mTimeOut = 1000;//默认超时时间未1s
	private final int LOAD_TIMEOUT = 0; //重新加载数据超时
	private final int UPDATE_SUCCESS =1; // 重新加载成功
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setOnScrollListener(this);
		mHeadView = initHeadView();
		addHeaderView(mHeadView);
		mHeadView.setPadding(0, - mHeadViewHeight, 0, 0);// 先设置headview 隐藏起来
	}

	/**
	 * 对外的接口函数
	 * 可以通过这个函数设置HeadView. 默认的headView 是通过initHeadView 函数得到的
	 * */
	public boolean setHeadView(View view) {
		if(view == null) return false;
		mHeadView = view;
		mHeadViewHeight = view.getMeasuredHeight();
		if(mHeadViewHeight <= 0) return false;
		return true;
	}

	private View initHeadView() {
		// TODO Auto-generated method stub
		ImageView mHeadImageView = new ImageView(mContext);//此处也可以直接设置Text文本, 也可以加载一个其他view 或者layout xml 文件
		mHeadImageView.setImageResource(R.drawable.ic_launcher);
		mHeadImageView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		mHeadImageView.setScaleType(ScaleType.FIT_XY);
		mHeadImageView.measure(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);//获取高度之前先measure 一下.
		mHeadViewHeight = mHeadImageView.getMeasuredHeight();//获得Head View 的高度
		return mHeadImageView;
	}

	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case LOAD_TIMEOUT:
				mPullState = PULL_BACK;
				moveHeadView(-mHeadViewHeight);
				break;
            case UPDATE_SUCCESS:
            	mPullState = PULL_BACK;
				moveHeadView(-mHeadViewHeight);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};
	/**
	 * 记录下当前的滚动状态
	 * */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		mCurrentScrollState = scrollState;
	}

	/**
	 * 根据滚动状态 设置 pull refresh的状态
	 * 
	 * 只有在 SCROLL_STATE_TOUCH_SCROLL 状态下并且在列表头位置的时候 才会去设置 refresh的状态值.
	 * */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
		if(mPullState == PULL_STOP){
			return;
		}
		mFristVisblePos = firstVisibleItem;
	}

	float mDownY;
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownY = ev.getY(); //记录下按下的位置
			break;
		case MotionEvent.ACTION_MOVE:
			float moveY = ev.getY();
			if(mFristVisblePos == 0) {
				if(mHeadView.getBottom() < mHeadViewHeight){
					mPullState = PULL_DOWN;
					moveHeadView(-(mHeadViewHeight - (int)(moveY - mDownY)));
				}
				else if(mHeadView.getBottom() >= mHeadViewHeight){
					mPullState = PULL_STOP;
				}
			}
			break;

		case MotionEvent.ACTION_UP:
			float endY = ev.getY();
			if(mFristVisblePos == 0) {
				mPullState = PULL_LOAD;
				mHandler.sendMessageDelayed(mHandler.obtainMessage(LOAD_TIMEOUT), mTimeOut);
				Thread updateThread = new Thread(updateDateRunable);
				updateThread.start();
			}
			break;

		default:
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	/**
	 * 更新数据的进程 每次都会重新启动一个
	 * 可以在这里处理耗时的网络操作
	 * */
	Runnable updateDateRunable = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				Thread.sleep(500); // 在这里处理必要的更新逻辑
				mHandler.sendMessage(mHandler.obtainMessage(UPDATE_SUCCESS));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	/**
	 * deal with HeadView action
	 * change as you want.
	 * */ 
	private void moveHeadView(int paddingTop) {
		mHeadView.setPadding(0, paddingTop, 0, 0);
	}
	
}
