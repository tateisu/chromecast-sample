
* receiver
chromecast上で動かすreceiverアプリのサンプルです。
HTMLファイルを適当なHTTPSサーバに置いて、
https://cast.google.com/publish/ でアプリと動作確認用chromecastデバイスを登録する必要があります。

* sender-android
Android上で動かすsenderアプリのサンプルです。
API level 9 (2.3,GingerBread) 以上で動作します。

以下のAndroid Library Projectが別途必要になります。
- android-support-v7-appcompat
- android-support-v7-mediarouter
- google_play_service

どれも build target を 4.2.2以上に設定する必要があります。
また、各プロジェクトが参照している android-support-v4.jar は同じバージョンのものになるようにアップデートします。


* Android端末の動作確認

Android 2.2.x の端末 (IS04)
- PlayストアのChromecastアプリは2.2.xには対応していません
- サンプルアプリの起動は可能です。MediaRouteButtonは常にグレーアウト状態になります。

Android 2.3.4の端末 (Motorola Photon,IS11M)
- PlayストアからChromecastアプリをインストールできました。
- サンプルアプリではPlayサービスと接続する時点で認証エラーやネットワークエラーが発生しました。原因は不明です。
(この端末はルート証明書が古いため、SSL通信などで問題があるのかもしれません)

Android 2.3.5の端末 (HTC Desire カスタムROM)
- PlayストアからChromecastアプリをインストールできました。
- サンプルアプリでChromecastと正常に接続出来ました。

Android 3.2.2 の端末 (Motorola XOOM2)
- PlayストアからChromecastアプリをインストールできました。
- サンプルアプリでChromecastと正常に接続出来ました。

Android (Walkman NW-F807)
- PlayストアからChromecastアプリをインストールできました。
- サンプルアプリでChromecastと正常に接続出来ました。

Android 4.3 の端末 (Galaxy Nexus)
- PlayストアからChromecastアプリをインストールできました。
- サンプルアプリでChromecastと正常に接続出来ました。

