# ChatGPT Reader

ChatGPT Reader は Pixel 8 単体で APK の取得、インストール、ChatGPT の読み上げ確認、診断レポート共有、修正版 APK の上書きインストールまで進められる Android アプリです。対象アプリは `com.openai.chatgpt` のみに限定しています。

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

## AccessibilityService を有効化する

1. ChatGPT Reader を開きます。
2. `AccessibilityService 設定を開く` をタップします。
3. Android 設定の `ユーザー補助` 画面で `ChatGPT Reader` を探してタップします。
4. `ChatGPT Reader を使用` を ON にします。
5. 確認画面の内容を確認し、許可します。
6. ChatGPT Reader に戻り、`AccessibilityService の状態: 有効` と表示されることを確認します。

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
4. 読み上げ速度を変えたい場合は、ChatGPT を開いたままクイック設定の `Reader Speed` タイルをタップします。次の読み上げ文から速度が反映されます。
5. ChatGPT Reader の通知が表示された場合は、通知から `一時停止`、`再開`、`停止`、`既読リセット` を操作できます。
6. 通知をタップすると ChatGPT Reader の設定画面に戻ります。

## 診断レポートを出力して共有する

1. ChatGPT Reader を開きます。
2. 通常は `診断用テキスト断片を含める ON / OFF` を OFF のままにします。
3. 開発者から依頼された場合だけ `診断用テキスト断片を含める ON / OFF` を ON にします。ON の場合でも候補テキストの先頭 80 文字までしか保存しません。
4. `診断レポートを出力` をタップします。
5. Android Share Sheet が開いたら、Gmail、Google Drive、Slack など共有先を選びます。
6. 共有後にログを消したい場合は `診断ログを消去` をタップします。

診断レポートには `generatedAt`、`appVersion`、`buildCommitSha`、端末情報、Android バージョン、AccessibilityService 状態、`accessibilityServiceConfigured`、`accessibilityServiceConnected`、Reader 状態、読み上げ速度、TTS 状態、`targetPackage`、`targetPackageDetected`、イベント数、候補数、除外数、読み上げ数、エラー、直近診断イベントが含まれます。

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
- AccessibilityService は `com.openai.chatgpt` のイベントだけを対象にしています。
