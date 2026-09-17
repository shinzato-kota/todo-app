# JUnitテストの結果

## 実施記録

| 項目 | 内容 |
|---|---|
| 実施日 | 2026-09-17 |
| 対象コミット | 96e5f1d |
| 実行コマンド | `.\mvnw.cmd test` |
| 実行環境 | Apache Maven 3.9.9 / Java 21.0.12.1(Eclipse Adoptium) |
| 結果 | 29件中29件合格 |

テストは実際のMariaDBに接続して実行している。各テストの変更はテスト終了時に取り消されるため、データベースの内容は変わらない。

## テストクラス別の件数

| テストクラス | 対象 | 件数 | 合格 |
|---|---|---|---|
| TodoAppApplicationTests | アプリケーションの起動 | 1 | 1 |
| TaskServiceTest | サービス(業務処理) | 9 | 9 |
| TaskControllerTest | 画面のコントローラー | 11 | 11 |
| TaskApiControllerTest | APIのコントローラー | 7 | 7 |
| TaskStatusTest | ステータスの定義 | 1 | 1 |
| 合計 | | 29 | 29 |

## テスト一覧

### TodoAppApplicationTests

| # | テストメソッド | 内容 | 結果 |
|---|---|---|---|
| 1 | `contextLoads` | アプリケーションが起動できる | 合格 |

### TaskServiceTest

| # | テストメソッド | 内容 | 結果 |
|---|---|---|---|
| 1 | `registerTask_setsStatusToTodo` | タスクを登録すると、ステータスが未着手になる | 合格 |
| 2 | `changeStatus_toDone_setsCompletedAt` | ステータスを完了に変更すると、完了日時が設定される | 合格 |
| 3 | `changeStatus_fromDone_clearsCompletedAt` | 完了から他のステータスへ変更すると、完了日時が解除される | 合格 |
| 4 | `changeStatus_toOnHold_doesNotSetCompletedAt` | 保留へ変更でき、完了日時は設定されない | 合格 |
| 5 | `changeStatus_toCancelled_doesNotSetCompletedAt` | 中止へ変更でき、完了日時は設定されない | 合格 |
| 6 | `removeTask_deletesFromDatabase` | 削除すると、指定したタスクだけがデータベースから取り除かれる | 合格 |
| 7 | `removeTask_notFound_throwsException` | 存在しないタスクを削除しようとすると例外が発生する | 合格 |
| 8 | `updateTask_changesTitle` | タスクのタイトルを更新できる | 合格 |
| 9 | `getTaskList_ordersByRegistration` | 一覧は登録した順に並ぶ | 合格 |

### TaskControllerTest

| # | テストメソッド | 内容 | 結果 |
|---|---|---|---|
| 1 | `create_emptyTitle_showsErrorAndDoesNotSave` | タイトルが空のまま保存しようとするとエラーになり、保存されない | 合格 |
| 2 | `create_validTitle_savesAndRedirects` | タイトルを入力して保存すると、一覧へ遷移して登録される | 合格 |
| 3 | `create_blankTitle_showsErrorAndDoesNotSave` | タイトルが空白だけの場合もエラーになり、保存されない | 合格 |
| 4 | `create_title100Chars_savesAndRedirects` | タイトルが100文字ちょうどなら保存できる | 合格 |
| 5 | `create_title101Chars_showsErrorAndDoesNotSave` | タイトルが101文字だとエラーになり、保存されない | 合格 |
| 6 | `list_addsStatusClassToRow` | 保留・中止のタスクは、表示スタイルを切り替えるためのクラスが行に付与される | 合格 |
| 7 | `list_noTasks_showsEmptyMessage` | タスクが0件のときは「タスクはありません」が表示される | 合格 |
| 8 | `editForm_existingTask_showsTitle` | 編集画面を開くと、登録済みのタイトルが表示される | 合格 |
| 9 | `editForm_notFound_redirectsToList` | 存在しないタスクの編集画面を開くと、一覧へ戻される | 合格 |
| 10 | `update_validTitle_updatesAndRedirects` | タイトルを変更して保存すると、一覧へ遷移し更新される | 合格 |
| 11 | `update_emptyTitle_showsErrorAndDoesNotUpdate` | 編集画面でタイトルを空にして保存すると、エラーになり更新されない | 合格 |

### TaskApiControllerTest

| # | テストメソッド | 内容 | 結果 |
|---|---|---|---|
| 1 | `updateStatus_toDone_savesCompletedAt` | 完了に変更すると、DONEを返し、完了日時が保存される | 合格 |
| 2 | `updateStatus_fromDone_clearsCompletedAt` | 完了以外へ変更すると、完了日時が解除される | 合格 |
| 3 | `updateStatus_nullStatus_returnsBadRequest` | ステータスが未指定の場合、400と項目別のエラーが返る | 合格 |
| 4 | `updateStatus_invalidStatus_returnsBadRequest` | 存在しないステータス値を送ると、400と内部情報を含まないエラーが返る | 合格 |
| 5 | `updateStatus_nonNumericId_returnsBadRequest` | URLのIDが数値でない場合も、400と内部情報を含まないエラーが返る | 合格 |
| 6 | `delete_returnsNoContent` | 削除するとデータベースから取り除かれ、204が返る | 合格 |
| 7 | `delete_notFound_returnsNotFound` | 存在しないタスクを削除しようとすると、404とメッセージが返る | 合格 |

### TaskStatusTest

| # | テストメソッド | 内容 | 結果 |
|---|---|---|---|
| 1 | `values_namesAreUnchanged` | 定数名が、外部と共有している値から変わっていない | 合格 |
