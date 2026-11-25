# JUnitテストの結果

## 実施記録

| 項目 | 内容 |
| --- | --- |
| 実施日 | 2026-11-24 |
| 実行コマンド | `.\mvnw.cmd clean test` |
| 実行環境 | Apache Maven 3.9.9 / Java 21.0.12.1(Eclipse Adoptium) |
| 結果 | 196件中196件合格 |

データベースを使うテストは、実際のMariaDBに接続して実行している。データベースに書き込むテストは、テストの終わりに変更を取り消すため、データベースの内容は変わらない。

## テストクラス別の件数

| テストクラス | 対象 | 件数 | 合格 |
| --- | --- | --- | --- |
| TodoAppApplicationTests | アプリケーションの起動 | 1 | 1 |
| ErrorPageTest | エラーページ | 4 | 4 |
| TaskControllerTest | タスクの画面のコントローラー | 60 | 60 |
| TaskApiControllerTest | タスクのAPIのコントローラー | 28 | 28 |
| CategoryControllerTest | カテゴリの画面のコントローラー | 15 | 15 |
| CategoryApiControllerTest | カテゴリのAPIのコントローラー | 4 | 4 |
| TaskServiceTest | タスクのサービス(業務処理) | 62 | 62 |
| TaskFormTest | タスクの追加・編集フォーム | 3 | 3 |
| TaskEntityMapperTest | タスクのエンティティとDTOの詰め替え | 6 | 6 |
| TaskFormMapperTest | タスクのフォームとDTOの詰め替え | 2 | 2 |
| TaskRequestMapperTest | タスクのAPIのリクエストとDTOの詰め替え | 6 | 6 |
| CategoryFormMapperTest | カテゴリのフォームとDTOの詰め替え | 2 | 2 |
| TaskStatusTest | ステータスの定義 | 1 | 1 |
| TaskPriorityTest | 優先度の定義 | 1 | 1 |
| DueDateStatusTest | 期限の状況の定義 | 1 | 1 |
| 合計 | | 196 | 196 |

## テスト一覧

テストケースと、それを確かめるテストメソッドの対応は [../test-basis.md](../test-basis.md) を参照。
