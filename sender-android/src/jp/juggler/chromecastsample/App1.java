package jp.juggler.chromecastsample;

import android.app.Application;

public class App1 extends Application {
	private ChromecastSender1 cast_sender;

	public ChromecastSender1 getChromecastSender() {
		if( cast_sender == null ) cast_sender = new ChromecastSender1( getApplicationContext() );
		return cast_sender;
	}

	public void exit_app() {
		if( cast_sender != null ){
			cast_sender.onExit();
			cast_sender = null;
		}
	}
}
