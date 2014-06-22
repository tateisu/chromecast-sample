package jp.juggler.chromecastsample;

import android.os.Bundle;
import android.view.View;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.MediaRouteButton;

public class ActMain extends  FragmentActivity {

	App1 app;

	// MediaRouteButton は FragmentActivity を必要とする
	MediaRouteButton mMediaRouteButton;

    @Override protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.act_main );

		app = (App1)getApplication();

		// MediaRouteButton には事前にセレクタを指定する
		mMediaRouteButton = (MediaRouteButton) findViewById(R.id.MediaRouteButton);
		mMediaRouteButton.setRouteSelector(app.getChromecastSender().mMediaRouteSelector);

		// メッセージを送信するボタン
		findViewById(R.id.btnMessage).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick( View v ) {
				app.getChromecastSender().message_bus_send( Long.toString( System.currentTimeMillis() ) );
			}
		} );
	}

	@Override protected void onResume() {
		super.onResume();
		// 画面表示のタイミングでアクティブスキャンを開始する
		app.getChromecastSender().onResume();
	}
	
	@Override protected void onPause() {
		super.onPause();
		// FIXME: アプリ終了のタイミングでコールバックの解放を行う。しかしアプリ終了という概念はアプリごとに異なる
		if( isFinishing() ) app.exit_app();
	}
}
