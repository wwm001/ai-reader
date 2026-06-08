# ChatGPT Reader

ChatGPT Reader は Pixel 8 単体で APK の取得、インストール、ChatGPT の読み上げ確認、診断レポート共有、修正版 APK の上書きインストールまで進められる Android アプリです。対象アプリは `com.openai.chatgpt` のみに限定しています。

## Ver.0.1.2 の操作

Ver.0.1.2 では、ChatGPT 画面上に Reader 専用の丸いフローティングボタンが表示されます。これは Android 側のアクセシビリティ用フローティングボタンとは別物です。

- 1回タップ: 再生 / 一時停止
- ダブルタップ: チャット先頭へ戻り、既読をリセットして最初から読み上げ
- 0.8秒以上、5秒未満の長押し後に指を離す: 速度メニュー表示
- 5秒押し続ける: Reader をシャットダウン
- ドラッグ: ボタン位置を移動。位置は保存されます。

5秒シャットダウンは Reader の音声、待機キュー、通知、専用フローティングボタンを停止します。Android の AccessibilityService 自体は無効化しません。再利用する場合は ChatGPT Reader アプリを開き、`Reader ON / OFF` を押すか、クイック設定の `ChatGPT Reader` タイルを ON にしてください。

速度メニューでは `0.8x`、`1.0x`、`1.2x`、`1.5x`、`2.0x` を選べます。速度はアプリ本体のスライダー、`Reader Speed` タイル、フローティングボタンの速度メニューで同期します。

## サービス構成

- `AccessibilityService`: ChatGPT アプリの画面内容だけを読み取り、読み上げ候補数、除外数、検出状態を診断に記録します。設定ファイルの `android:packageNames` は `com.openai.chatgpt` のみです。
- `TileService`: Pixel 8 のクイック設定から Reader の ON/OFF を切り替えます。長押しはアプリ設定画面を開くための QS tile preferences intent に対応しています。
- `SpeedTileService`: クイック設定から読み上げ速度を `0.8x → 1.0x → 1.2x → 1.5x → 2.0x` の順に切り替えます。
- 通知: Reader が有効または読み上げ状態のときだけ低優先度通知を表示します。通知アクションは一時停止、再開、停止、既読リセットです。不要な常駐サービスは追加していません。
- 診断レポート: ユーザーが `診断レポートを出力` を押した場合のみ cache 配下に UTF-8 JSON を作成し、Android Share Sheet で手動共有します。外部サーバーへの自動送信は行わず、`INTERNET` 権限も使いません。

## Pixel 8 だけで APK を取得する

1. Pixel 8 で Chrome を開き、GitHub のこのリポジトリページを開きます。
2. 画面上部の `Actions` をタップします。表示が狭い場合は、リポジトリ名の下にあるタブ列を横スクロールして `Actions` を探します。
3. 左側または上部の workflow 一覧で `Android Debug Build` を選びます。
4. 一番上の成功している実行をタップします。緑のチェックが付いている実行を選んでください。
5. 実行詳細画面を下へスクロールし、`Artifacts` の `chatgpt-reader-debug-apk` をタップします。
6. ZIP がダウンロードされたら、Pixel 8 の `Files` アプリを開きます。
7. `Downloads` を開き、`chatgpt-reader-debug-apk.zip` をタップして展開します。
8. 展開された `app-debug.apk` をタップします。

## Pixel 8 に APK をインストールする

1. `app-debug.apk` をタップします。
2. `セキュリティ上の理由から...` のような表示が出た場合は `設定` をタップします。
3. `この提供元のアプリを許可` を ON にします。提供元は通常 `Files` または `Chrome` です。
4. 戻る操作で APK 画面に戻り、`インストール` をタップします。
5. インストール完了後、`開く` をタップします。

上書きインストール後は、ChatGPT Reader を開き、`現在のアプリバージョン: 0.1.2 (3)` と新しい `build commit SHA` が表示されることを確認してください。

## AccessibilityService を有効化する

1. ChatGPT Reader を開きます。
2. `AccessibilityService 設定を開く` をタップします。
3. Android 設定の `ユーザー補助` 画面で `ChatGPT Reader` を探してタップします。
4. `ChatGPT Reader を使用` を ON にします。
5. 確認画面の内容を確認し、許可します。
6. ChatGPT Reader に戻り、`AccessibilityService の状態: 有効` と表示されることを確認します。

Android 側のアクセシビリティ用フローティングボタンが不要な場合は、同じユーザー補助画面で `ChatGPT Reader のショートカット` を OFF にできます。Reader 専用ボタンはアプリ側が ChatGPT 画面に表示する別のボタンなので、Android 側ショートカットをOFFにしても Reader 専用ボタンの操作には影響しません。

## クイック設定へ Reader タイルを追加する

1. Pixel 8 の画面上端から 2 回下へスワイプして、クイック設定を大きく表示します。
2. 鉛筆アイコンをタップします。
3. 下の未追加タイル一覧から `ChatGPT Reader` と `Reader Speed` を探します。
4. `ChatGPT Reader` と `Reader Speed` を長押しして、上のクイック設定エリアへドラッグします。
5. 戻る操作で編集を終了します。
6. ChatGPT を開いたままクイック設定を下ろし、`ChatGPT Reader` タイルをタップすると ON/OFF を切り替えられます。
7. `Reader Speed` タイルをタップすると読み上げ速度を切り替えられます。

タイル表示は次の意味です。

- `有効`: Reader ON
- `無効`: Reader OFF
- `AccessibilityService 未設定`: ユーザー補助設定がまだ OFF

Reader を OFF にすると、現在の音声と待機中の読み上げキューは即時停止します。`一時停止` は通知を残し、通知の `再開` から戻せます。`停止` は通知を消します。

## ChatGPT で読み上げ確認する

1. Pixel 8 で ChatGPT アプリを開きます。
2. ChatGPT Reader のクイック設定タイルを `有効` にします。
3. ChatGPT の会話画面を表示し、回答文が表示される状態にします。
4. 画面右下付近に Reader 専用フローティングボタンが表示されることを確認します。
5. 専用ボタンを1回タップし、再生 / 一時停止が切り替わることを確認します。
6. 同じ画面のままもう一度タップし、続きから再開することを確認します。
7. 読み上げ速度を変えたい場合は、専用ボタンを長押しして速度メニューを開くか、クイック設定の `Reader Speed` タイルをタップします。
8. ChatGPT Reader の通知が表示された場合は、通知から `一時停止`、`再開`、`停止`、`既読リセット` を操作できます。
9. 通知をタップすると ChatGPT Reader の設定画面に戻ります。

## 診断レポートを出力して共有する

1. ChatGPT Reader を開きます。
2. 通常は `診断用テキスト断片を含める ON / OFF` を OFF のままにします。
3. 開発者から依頼された場合だけ `診断用テキスト断片を含める ON / OFF` を ON にします。ON の場合でも候補テキストの先頭 80 文字までしか保存しません。
4. `診断レポートを出力` をタップします。
5. Android Share Sheet が開いたら、Gmail、Google Drive、Slack など共有先を選びます。
6. 共有後にログを消したい場合は `診断ログを消去` をタップします。

診断レポートには `generatedAt`、`appVersion`、`buildCommitSha`、端末情報、Android バージョン、AccessibilityService 状態、`accessibilityServiceConfigured`、`accessibilityServiceConnected`、Reader 状態、読み上げ速度、TTS 状態、`targetPackage`、`targetPackageDetected`、イベント数、候補数、除外数、読み上げ数、エラー、直近診断イベントが含まれます。

Ver.0.1.2 では、さらに `overlayVisible`、`overlayPosition`、`pendingQueueSize`、`spokenFingerprintCount`、`pendingFingerprintCount`、`currentlySpeakingFingerprint`、`readCursor`、フローティングボタン操作回数、スクロール先頭移動回数、snapshot skip、除外理由別件数も出力します。

## 修正版 APK を上書きインストールする

1. 開発者から修正版の GitHub Actions 実行が完了した連絡を受けたら、Pixel 8 の Chrome で GitHub リポジトリを開きます。
2. `Actions` → `Android Debug Build` → 最新の成功実行を開きます。
3. `Artifacts` の `chatgpt-reader-debug-apk` をダウンロードします。
4. `Files` アプリの `Downloads` で ZIP を展開します。
5. 新しい `app-debug.apk` をタップします。
6. `更新しますか？` と表示されたら `インストール` をタップします。
7. 完了後、ChatGPT Reader を開いて `現在のアプリバージョン` と `build commit SHA` を確認します。

## 権限方針

- `INTERNET` 権限は追加していません。
- スクリーンショット取得権限は追加していません。
- ジェスチャー送信権限は追加していません。
- `SYSTEM_ALERT_WINDOW` 権限は追加していません。Reader 専用ボタンは AccessibilityService の `TYPE_ACCESSIBILITY_OVERLAY` で表示します。
- AccessibilityService は `com.openai.chatgpt` のイベントだけを対象にしています。

## Pixel 8 で要確認

次の項目は実機で確認してください。PCやADBなしで、Pixel 8上の操作と診断レポートで判定します。

- 上書きインストール後、`Ver.0.1.2 (3)` と新しいSHAが表示される
- ChatGPT画面右下にReader専用ボタンが表示される
- 専用ボタンをドラッグで移動でき、位置が復元される
- 1回タップで再生 / 一時停止が切り替わる
- 一時停止後、同じ画面で続きを読める
- Reader OFF → ON で同じ画面の未読から再開する
- ダブルタップでチャット先頭へ戻り、最初から読む
- 長押しで速度メニューが出る
- 速度選択後、体感上すぐ速度が変わる
- 5秒押しで進行表示後にReaderがシャットダウンする
- シャットダウン後、ChatGPT ReaderアプリまたはReaderタイルから再利用できる
- 上部タイトル、プロジェクト名、入力欄、モデル名、ボタンを読まない
- 手動下スクロールで新規未読だけ読む
- 手動上スクロールで読み済み部分を大量に再読しない
- 回答生成中に書きかけ末尾を何度も読まない
- 診断JSONをPixel 8単体で共有できる
