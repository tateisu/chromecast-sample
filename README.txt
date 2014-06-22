
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
