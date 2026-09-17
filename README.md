# Todoアプリ

Java 21 / Spring Boot / Thymeleafで作成したタスク管理アプリです。4つのフェーズに分けて段階的に開発しており、現在はフェーズ1（タスクの一覧・追加・編集・削除とステータス管理）まで実装しています。

## 使用技術
- Java 21 / Spring Boot 3.5.x / Thymeleaf
- JavaScript / jQuery
- MariaDB

## 開発フェーズ
1. タスクの一覧・追加・編集・削除とステータス管理
2. 期限・優先度・絞り込み・分類・並び替え
3. サブタスク・繰り返し
4. 認証・通知・API化

## 構成
```
src/main/java/com/shinzato/todoapp/
  controller/  画面用・API用のコントローラー
  service/     業務処理
  repository/  DBアクセス
  entity/      JPAエンティティ
  enums/       列挙型
  dto/         APIの入出力と一覧表示に使う読み取り専用のデータ
  form/        画面の入力フォームで扱うデータ
  exception/   例外クラス・例外ハンドラー
src/main/resources/
  templates/   画面のHTML(Thymeleaf)
  static/      CSS・JavaScript
src/test/java/com/shinzato/todoapp/
               JUnitテスト
db/            DDL(データベース・テーブル定義)・動作確認用データ
docs/
  manual-test.md  手動テストの手順(JUnitで検証できないブラウザ上の動作など)
  test-results/   JUnitテストと手動テストの結果
```

## セットアップ

1. MariaDBで`db/01_schema.sql`を実行し、`tododb`データベースと`task`テーブルを作成する
2. MariaDBで`db/02_sample_data.sql`を実行し、動作確認用のタスクを登録する
3. アプリ接続用のDBユーザーを作成し、`tododb`への権限を付与する
4. `application-local.properties.example`を`application-local.properties`にコピーし、DB接続用のユーザーとパスワードを設定する
5. 次のコマンドで起動し、`http://localhost:8080/todo-app/tasks`にアクセスする
   - Windows: `.\mvnw.cmd spring-boot:run`
   - macOS・Linux: `./mvnw spring-boot:run`

終了するには、起動したターミナルで`Ctrl + C`を押す。

手順2で登録されるのは5つのステータスそれぞれのタスクで、一覧画面で表示スタイルの違いを確認できる。データが不要な場合は手順2を飛ばしてよい。
