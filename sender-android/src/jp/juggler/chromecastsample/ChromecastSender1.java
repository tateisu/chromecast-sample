package jp.juggler.chromecastsample;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class ChromecastSender1 {
	// Log出力用のタグ
	static final String TAG = "ChromecastSender1";

	// Google Cast SDK Developer Consoleから取得した Application ID
	public static final String CAST_APPLICATION_ID = "0449183F";

	// メッセージ交換に使用する名前空間
	public static final String message_namespace = "urn:x-cast:jp.juggler.TestChromecastAudio.channel";

	public final Context context;
	public final MediaRouter mMediaRouter;
	public final MediaRouteSelector mMediaRouteSelector;

	public ChromecastSender1(Context context) {
		Log.d( TAG, "ctor" );

		this.context = context;
		this.mMediaRouter = MediaRouter.getInstance( context.getApplicationContext() );
		// セレクタは手動スキャン開始時に指定したり、MediaRouteButtonに設定したりする
		this.mMediaRouteSelector =
			new MediaRouteSelector.Builder()
				.addControlCategory( CastMediaControlIntent.categoryForCast( CAST_APPLICATION_ID ) )
				.build();
	}

	////////////////////////////////////////////////////////////////////
	// UIのライフサイクルイベントから適切に呼び出すこと

	WeakReference<Activity> last_activity;

	public void onResume( Activity activity ) {
		Log.d( TAG, "onResume" );
		last_activity = new WeakReference<Activity>( activity );
		route_scan();
	}

	public void onExit() {
		Log.d( TAG, "onExit" );
		mMediaRouter.removeCallback( cb_media_router );
	}

	////////////////////////////////////////////////////////////////////
	// MediaRoute関係の処理

	// デフォルトの出力先を選択し直す
	public void route_unselect() {
		try{
			// デフォルトルートを選択し直す
			for( RouteInfo ri : mMediaRouter.getRoutes() ){
				if( ri.isDefault() ){
					mMediaRouter.selectRoute( ri );
					break;
				}
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
	}

	// スキャンの開始
	public void route_scan() {
		// Activityが再生成された場合などでは、既に選択された出力先が存在する
		RouteInfo ri = mMediaRouter.getSelectedRoute();
		if( ri != null && ri.matchesSelector( mMediaRouteSelector ) ){
			Log.d( TAG, "scan: route is already selected." );
			cb_media_router.onRouteSelected( mMediaRouter, ri );
		}
		// アクティブスキャンの開始
		mMediaRouter.addCallback( mMediaRouteSelector, cb_media_router, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN );
	}

	// スキャン結果のコールバック
	final MediaRouter.Callback cb_media_router = new MediaRouter.Callback() {
		// 出力先が選択された
		@Override public void onRouteSelected( MediaRouter mr, RouteInfo ri ) {
			Log.d( TAG, "onRouteSelected routeId=" + ri.getId() );
			if( ri.isDefault() ) return;
			try{
				CastDevice device = CastDevice.getFromBundle( ri.getExtras() );
				client_open( device );
			}catch( Throwable ex ){
				ex.printStackTrace();
			}
		}

		// 出力先の選択が解除された
		@Override public void onRouteUnselected( MediaRouter mr, RouteInfo ri ) {
			Log.d( TAG, "onRouteUnselected routeId=" + ri.getId() );
			if( ri.isDefault() ) return;
			// クライアントとの接続状態をクリアする
			client_close();
		}

		// 出力先が除去された
		@Override public void onRouteRemoved( MediaRouter mr, RouteInfo ri ) {
			Log.d( TAG, "onRouteRemoved routeId=" + ri.getId() );
			if( ri.isDefault() ) return;
			// chromecastとの接続が切れたらMediaRouteButtonがグレーアウトするので、再度スキャンさせて再接続できるようにする
			route_scan();
		}
	};

	/////////////////////////////////////////////////////////////
	// Chromecast との接続

	// 選択された出力デバイス
	CastDevice mSelectedDevice;

	// デバイスのAPIクライアント
	GoogleApiClient mApiClient;

	void client_close() {
		try{
			if( mSelectedDevice != null ){
				if( mApiClient != null ){
					if( mApiClient.isConnected() ){
						message_bus_remove();
						mApiClient.disconnect();
					}
					mApiClient = null;
				}
				mSelectedDevice = null;
			}
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
	}

	void client_open( CastDevice device ) {
		// 既に接続しているなら何もしない
		if( mSelectedDevice != null ){
			Log.d( TAG, "client_open: already selected." );
			return;
		}

		// 既存の接続情報をクリアする
		client_close();

		// 接続処理中に接続開始処理を繰り返さないようにするため、選択されたデバイスを覚えておく
		mSelectedDevice = device;

		// Play開発者サービスの状態を確認する
		int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable( context );
		if( errorCode != ConnectionResult.SUCCESS ){
			Log.d( TAG, "isGooglePlayServicesAvailable failed.err=" + errorCode );
			route_unselect();
			GooglePlayServicesUtil.getErrorDialog( errorCode, last_activity.get(), 10 ).show();
			return;
		}

		// chromecastとの接続処理を開始する
		Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder( device, mCastClientListener );
		apiOptionsBuilder.setVerboseLoggingEnabled( true );
		mApiClient = new GoogleApiClient.Builder( context )
			.addApi( Cast.API, apiOptionsBuilder.build() )
			.addConnectionCallbacks( cb_google_api )
			.addOnConnectionFailedListener( mConnectionFailedListener )
			.build();
		mApiClient.connect();
	}

	// GoogleAPIの接続コールバック
	final GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
		@Override public void onConnectionFailed( ConnectionResult result ) {
			Log.d( TAG, "GoogleApiClient onConnectionFailed result=" + result.toString() );
			route_unselect();
			//
			int error_code = result.getErrorCode();
			GooglePlayServicesUtil.getErrorDialog( error_code, last_activity.get(), 10 ).show();
		}
	};

	// GoogleAPIの接続コールバック
	final GoogleApiClient.ConnectionCallbacks cb_google_api = new GoogleApiClient.ConnectionCallbacks() {
		@Override public void onConnectionSuspended( int cause ) {
			Log.d( TAG, "GoogleApiClient onConnectionSuspended cause=" + cause );
			route_unselect();
		}

		@Override public void onConnected( Bundle connectionHint ) {
			Log.d( TAG, "GoogleApiClient onConnected" );

			try{
				// ApplicationMetadata app = Cast.CastApi.getApplicationMetadata( mApiClient );
				// なぜか常にnullが返るようだ。。。？

				// レシーバ上でアプリケーションを開始する
				// (既にレシーバー上で該当アプリが動いている場合、それがそのまま使われる）
				Cast.CastApi.launchApplication( mApiClient, CAST_APPLICATION_ID, false )
					.setResultCallback( new ResultCallback<Cast.ApplicationConnectionResult>() {
						@Override public void onResult( ApplicationConnectionResult result ) {
							Status status = result.getStatus();
							Log.d( TAG, "launchApplication status=" + status );
							if( ! status.isSuccess() ){
								Log.e( TAG, "launchApplication failed.status=" + status );
								route_unselect();
								return;
							}
							if( ! message_bus_regist() ){
								Log.e( TAG, "message bus connection failed." );
								route_unselect();
								return;
							}
						}
					} );
			}catch( Throwable ex ){
				ex.printStackTrace();
				route_unselect();
			}
		}

	};

	final Cast.Listener mCastClientListener = new Cast.Listener() {
		// Called when the connection to the receiver application has been lost, such as when another client has launched a new application.
		@Override public void onApplicationDisconnected( int statusCode ) {
			Log.d( TAG, "Cast.onApplicationDisconnected status=" + statusCode );
			route_unselect();
		}

		// Called when the status of the connected application has changed. 
		@Override public void onApplicationStatusChanged() {
			String status = null;
			if( mApiClient != null ){
				status = Cast.CastApi.getApplicationStatus( mApiClient );
			}
			Log.d( TAG, "Cast.onApplicationStatusChanged status=" + status );
		}

		// Called when the device's volume or mute state has changed. 
		@Override public void onVolumeChanged() {
			Log.d( TAG, "Cast.onVolumeChanged" );
		}
	};

	//////////////////////////////////////////////////////////////////////
	// chromecast receiver app とのメッセージ交換

	boolean message_bus_regist() {
		Log.d( TAG, "message_bus_regist" );
		try{
			Cast.CastApi.setMessageReceivedCallbacks(
				mApiClient
				, message_namespace
				, cb_message
				);
			message_bus_send( "message bus connected." );
			return true;
		}catch( Throwable ex2 ){
			ex2.printStackTrace();
			return false;
		}
	}

	void message_bus_remove() {
		Log.d( TAG, "message_bus_remove" );
		try{
			Cast.CastApi.removeMessageReceivedCallbacks( mApiClient, message_namespace );
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
	}

	final Cast.MessageReceivedCallback cb_message = new Cast.MessageReceivedCallback() {
		@Override public void onMessageReceived( CastDevice castDevice, String namespace, String message ) {
			Log.d( TAG, "onMessageReceived: " + message );
		}
	};

	void message_bus_send( final String message ) {
		if( mApiClient == null ){
			Log.d( TAG, "message_bus_send: not connected." );
			return;
		}
		try{
			Cast.CastApi.sendMessage( mApiClient, message_namespace, message )
				.setResultCallback(
					new ResultCallback<Status>() {
						@Override public void onResult( Status result ) {
							if( ! result.isSuccess() ){
								Log.e( TAG, "Sending message failed" );
							}else{
								Log.d( TAG, "SEND " + message );
							}
						}
					}
				);
		}catch( Throwable ex ){
			ex.printStackTrace();
		}
	}

}
