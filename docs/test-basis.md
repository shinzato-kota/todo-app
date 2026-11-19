# テストケースの根拠

フェーズ1とフェーズ2で作ったすべての機能について、テストケースをどう選んだかと、それを確かめるテストを示す。

## 0. この資料について

### 使った技法

| 技法 | 使った場面 |
| --- | --- |
| 同値分割法 | 入力の値を、同じように処理されるまとまり（同値パーティション）に分け、まとまりごとに代表の値を1つ試す |
| 境界値分析 | 文字数などの上限・下限の前後（ちょうどの値と、1つ外れた値）を試す |
| デシジョンテーブル | 2つ以上の条件の組み合わせで結果が変わるもの（期限の状況、完了日時、期限の絞り込み）を、組み合わせごとに試す |

### 表の読み方

- 1つの表には、1つの技法のテストケースだけを書く。同値分割法と境界値分析は、同じ入力項目でも別の表にし、見出しのかっこに技法の名前を書く
- 同値分割法の表は、同値パーティションごとに1行にし、有効か無効かを書く。境界値分析の表は、境界ごとに、境界の値と、隣の同値パーティションの一番近い値を1行ずつ書く
- どの技法の表にも当たらないテストは、各節の「そのほかのテストケース」に書く
- 「確かめるテスト」の列には、JUnitのテストメソッドの名前か、手動テストの項目の番号を書く
- JUnitのテストは `src/test/java` にある。テストメソッドの名前の前に、テストクラスの名前を書く
- 手動テストの項目は [manual-test.md](manual-test.md) の見出しの番号で、「手動1」のように書く

### アプリ全体

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| アプリを起動する | 起動できる | `TodoAppApplicationTests.contextLoads` |

## 1. タスク一覧の表示

### 期限の状況（デシジョンテーブル）

期限の状況は、期限とステータスの組み合わせで決まる。

| 期限 | ステータス | 期限の状況 | 確かめるテスト |
| --- | --- | --- | --- |
| なし | 問わない | 期限切れでも今日が期限でもない | `TaskServiceTest.getTaskList_noDueDate_returnsNormal` |
| 昨日以前 | 未着手・進行中・保留 | 期限切れ | `TaskServiceTest.getTaskList_pastDueTodo_returnsOverdue`、`TaskServiceTest.getTaskList_pastDueInProgress_returnsOverdue`、`TaskServiceTest.getTaskList_pastDueOnHold_returnsOverdue` |
| 昨日以前 | 完了・中止 | 期限切れでも今日が期限でもない | `TaskServiceTest.getTaskList_pastDueDone_returnsNormal`、`TaskServiceTest.getTaskList_pastDueCancelled_returnsNormal` |
| 今日 | 問わない | 今日が期限 | `TaskServiceTest.getTaskList_dueTodayTodo_returnsDueToday`、`TaskServiceTest.getTaskList_dueTodayInProgress_returnsDueToday`、`TaskServiceTest.getTaskList_dueTodayOnHold_returnsDueToday`、`TaskServiceTest.getTaskList_dueTodayDone_returnsDueToday`、`TaskServiceTest.getTaskList_dueTodayCancelled_returnsDueToday` |
| 明日以降 | 問わない | 期限切れでも今日が期限でもない | `TaskServiceTest.getTaskList_dueTomorrowTodo_returnsNormal` |

### 期限の状況（境界値分析）

| 境界 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- |
| 期限切れと今日が期限の境界 | 昨日（未着手） | 期限切れ | `TaskServiceTest.getTaskList_pastDueTodo_returnsOverdue` |
| 期限切れと今日が期限の境界、今日が期限と期限切れでもない状況の境界 | 今日（未着手） | 今日が期限 | `TaskServiceTest.getTaskList_dueTodayTodo_returnsDueToday` |
| 今日が期限と期限切れでもない状況の境界 | 明日（未着手） | 期限切れでも今日が期限でもない | `TaskServiceTest.getTaskList_dueTomorrowTodo_returnsNormal` |

### タスクの件数（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 1件以上 | 有効 | タスクを1件足す | 「タスクはありません」が表示されない | `TaskControllerTest.list_withTasks_hidesEmptyMessage` |
| 0件 | 有効 | タスクをすべて消す | 「タスクはありません」が表示される | `TaskControllerTest.list_noTasks_showsEmptyMessage` |

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 期限切れのタスク | タスクの行に期限切れのクラスが付き、「期限切れ」のラベルが出力される | `TaskControllerTest.list_overdueTask_addsOverdueClassAndLabel` |
| 今日が期限のタスク | タスクの行に今日が期限のクラスが付く | `TaskControllerTest.list_dueTodayTask_addsDueTodayClass` |
| 期限切れ・今日が期限のタスクの見た目 | 期限切れは赤字とラベル、今日が期限はオレンジの太字で表示される | 手動38 |
| 空白のない長いタイトル（「⌨️」50個、半角英字100文字、ひらがな100文字） | タイトルの列の中で折り返して表示され、右の列に重ならない | 手動40 |
| 色のあるカテゴリのタスク | カテゴリの列に、その色の印と名前が出る | `TaskControllerTest.list_categoryWithColor_showsColorMarkAndName`、`TaskEntityMapperTest.toSummary_taskWithCategory_copiesCategoryNameAndColor` |
| 色なしのカテゴリのタスク | カテゴリの列に、印を見えなくするクラスと名前が出る | `TaskControllerTest.list_categoryWithoutColor_showsNameWithHiddenMark` |
| カテゴリなしのタスク | カテゴリの列に何も出さない | `TaskControllerTest.list_noCategory_omitsCategoryLabel`、`TaskEntityMapperTest.toSummary_taskWithoutCategory_setsNullCategory` |
| 一覧の並び | 表示順に並ぶ | `TaskServiceTest.getTaskList_ordersBySortOrder` |
| 詳細・優先度・期限のあるタスク | 詳細・優先度・期限が表示される | `TaskControllerTest.list_showsDescriptionPriorityAndDueDate`、`TaskEntityMapperTest.toSummary_copiesDescriptionPriorityDueDateAndDueDateStatus` |
| 詳細・期限のないタスク | 詳細と期限の要素を出力しない | `TaskControllerTest.list_noDescriptionAndDueDate_omitsElements` |
| 保留・中止のタスク | タスクの行とステータスのプルダウンに、ステータスのクラスが付く | `TaskControllerTest.list_addsStatusClassToRowAndStatusSelect` |
| 期限の状況が無いまま一覧表示用のデータを作る | 作らずに例外が発生する | `TaskEntityMapperTest.toSummary_nullDueDateStatus_throwsNullPointerException` |
| 知らせる文を渡されて一覧を開く | その文がJavaScriptへ渡す属性に出力される | `TaskControllerTest.list_withAlertMessage_outputsAlertMessageAttribute` |
| 知らせる文が無く一覧を開く | JavaScriptへ渡す属性を出力しない | `TaskControllerTest.list_withoutAlertMessage_omitsAlertMessageAttribute` |
| 期限の状況の定数名 | 外部と共有している値から変わっていない | `DueDateStatusTest.values_namesAreUnchanged` |
| タスクとカテゴリを登録し、タスクを並び替えてからアプリを再起動する | タスクとカテゴリと並び替えた後の順番が残っている | 手動1 |

## 2. タスクの追加・編集

### タイトル（同値分割法）

上限は100文字。空白だけのタイトルは、入力が無いものとして扱う。

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 空白だけ（入力なし） | 無効 | 空文字、半角空白だけ、全角空白だけ | エラー、保存しない | `TaskControllerTest.create_emptyTitle_showsErrorAndDoesNotSave`、`TaskControllerTest.update_emptyTitle_showsErrorAndDoesNotUpdate`、`TaskControllerTest.create_blankTitle_showsErrorAndDoesNotSave`、`TaskControllerTest.create_fullWidthSpaceTitle_showsErrorAndDoesNotSave` |
| 1〜100文字 | 有効 | 11文字 | 保存する | `TaskControllerTest.create_validTitle_savesAndRedirects`、`TaskControllerTest.update_validTitle_updatesAndRedirects` |
| 101文字以上 | 無効 | 101文字 | エラー、保存しない | `TaskControllerTest.create_title101Chars_showsErrorAndDoesNotSave` |

### タイトル（境界値分析）

| 境界 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- |
| 下限 | 0文字（空文字） | エラー、保存しない | `TaskControllerTest.create_emptyTitle_showsErrorAndDoesNotSave` |
| 下限 | 1文字 | 保存する | `TaskControllerTest.create_title1Char_savesAndRedirects` |
| 上限 | 100文字 | 保存する | `TaskControllerTest.create_title100Chars_savesAndRedirects` |
| 上限 | 101文字 | エラー、保存しない | `TaskControllerTest.create_title101Chars_showsErrorAndDoesNotSave` |

### 詳細（同値分割法）

上限は2000文字。改行（CR+LF）はLFにそろえてから数える。

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 空白と改行だけ（入力なし） | 有効 | 空白と改行だけ | 詳細なし（null）にする | `TaskFormTest.setDescription_blank_setsNull` |
| 1〜2000文字 | 有効 | 「1行目」「2行目」の2行 | その詳細で保存する | `TaskControllerTest.create_withDescriptionDueDateAndPriority_savesValues` |
| 2001文字以上 | 無効 | 2001文字 | エラー、保存しない | `TaskControllerTest.create_description2001Chars_showsErrorAndDoesNotSave` |

### 詳細（境界値分析）

下限は無い（入力なしも有効）。

| 境界 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- |
| 上限 | 改行を含めて2000文字（送信時は2001文字） | 保存する | `TaskControllerTest.create_description2000CharsWithLineBreak_saves` |
| 上限 | 2001文字 | エラー、保存しない | `TaskControllerTest.create_description2001Chars_showsErrorAndDoesNotSave` |

### 期限・優先度（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 期限が日付 | 有効 | `2026-10-31` | その日付で保存する | `TaskControllerTest.create_withDescriptionDueDateAndPriority_savesValues` |
| 期限が日付でない | 無効 | 「明日」 | エラー、保存しない | `TaskControllerTest.create_invalidDueDate_showsErrorAndDoesNotSave` |
| 期限が空 | 有効 | 空文字 | 期限なしで保存する | `TaskControllerTest.create_emptyDueDate_savesWithoutDueDate` |
| 期限の項目が無い | 有効 | （送らない） | 期限なしで保存する | `TaskControllerTest.create_validTitle_savesAndRedirects` |
| 優先度が「高」「中」「低」のどれか | 有効 | `HIGH` | その優先度で保存する | `TaskControllerTest.create_withDescriptionDueDateAndPriority_savesValues` |
| 優先度が存在しない値 | 無効 | `URGENT` | エラー、保存しない | `TaskControllerTest.create_unknownPriority_showsErrorAndDoesNotSave` |
| 優先度が空 | 無効 | 空文字 | エラー、保存しない | `TaskControllerTest.create_emptyPriority_showsErrorAndDoesNotSave` |
| 優先度の項目が無い | 無効 | （送らない） | エラー、保存しない | `TaskControllerTest.create_missingPriority_showsErrorAndDoesNotSave` |

### カテゴリ（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 存在するカテゴリのID | 有効 | 登録したカテゴリのID | そのカテゴリで保存する | `TaskControllerTest.create_withCategory_savesCategory` |
| 空（「カテゴリなし」） | 有効 | 空文字 | カテゴリなしで保存する | `TaskControllerTest.create_emptyCategory_savesWithoutCategory`、`TaskControllerTest.update_toNoCategory_removesCategory` |
| 項目が無い | 有効 | （送らない） | カテゴリなしで保存する | `TaskControllerTest.create_missingCategory_savesWithoutCategory` |
| 存在しないカテゴリのID | 無効 | 削除したカテゴリのID | カテゴリの欄にエラー、保存しない | `TaskControllerTest.create_deletedCategory_showsErrorAndDoesNotSave`、`TaskControllerTest.update_deletedCategory_showsErrorAndDoesNotUpdate` |
| 数値でない値 | 無効 | `abc` | エラー、保存しない | `TaskControllerTest.create_nonNumericCategory_showsErrorAndDoesNotSave` |

### 編集するタスク（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 存在するタスクの編集画面を開く | 有効 | 登録したタスクのID | 登録済みの値が表示される | `TaskControllerTest.editForm_existingTask_showsTitle`、`TaskControllerTest.editForm_existingTask_showsDescriptionDueDateAndPriority`、`TaskControllerTest.editForm_taskWithCategory_selectsSavedCategory` |
| 存在しないタスクの編集画面を開く | 無効 | `-1`（手動11は、別の画面で削除したタスクのID） | 一覧へ戻され、見つからないことを知らせる | `TaskControllerTest.editForm_notFound_redirectsToListWithAlertMessage`、手動11 |
| 存在しないタスクを保存する | 無効 | 削除したタスクのID | 一覧へ戻され、見つからないことを知らせる | `TaskControllerTest.update_notFound_redirectsToListWithAlertMessage`、手動12 |

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 存在しないカテゴリのIDと、空のタイトルを一緒に送る | 2つのエラーが両方出る | `TaskControllerTest.create_deletedCategoryAndEmptyTitle_showsBothErrors` |
| 詳細の前後に空白がある | 空白を削らずに残す | `TaskFormTest.setDescription_leadingAndTrailingSpaces_keepsAsIs` |
| 詳細の改行がCR+LF | LFにそろえる | `TaskFormTest.setDescription_crlf_convertsToLf` |
| 詳細に改行と絵文字を含む | そのまま保存され、一覧に表示される | `TaskControllerTest.create_descriptionWithLineBreakAndEmoji_savesAsIs`、手動20（表示だけ）、手動39 |
| 追加画面を開く | 優先度は「中」が選ばれている | `TaskControllerTest.newForm_selectsMediumPriority` |
| 追加画面のカテゴリのプルダウン | どの選択肢も選ばれておらず、先頭が「カテゴリなし」 | `TaskControllerTest.newForm_selectsNoCategoryAtTop` |
| 追加画面のカテゴリの並び | 作成した順に並ぶ | `TaskControllerTest.newForm_listsCategoriesInCreatedOrder` |
| タスクを登録する | ステータスが未着手になる | `TaskServiceTest.registerTask_setsStatusToTodo` |
| タスクを登録する | 詳細・期限・優先度も保存される | `TaskServiceTest.registerTask_savesDescriptionDueDateAndPriority` |
| タスクを登録する | 今あるどのタスクよりも後に並ぶ | `TaskServiceTest.registerTask_placesAfterExistingTasks` |
| タスクを更新する | タイトルが変わる | `TaskServiceTest.updateTask_changesTitle` |
| タスクを更新する | 詳細・期限・優先度も変わる | `TaskServiceTest.updateTask_changesDescriptionDueDateAndPriority` |
| フォームから入力のデータを作る | すべての項目が移される | `TaskFormMapperTest.toInput_copiesAllItems` |
| 編集画面のデータからフォームを作る | すべての項目が移される | `TaskFormMapperTest.toForm_copiesAllItems` |
| 編集画面のデータを作る | 詳細・期限・優先度も移される | `TaskEntityMapperTest.toEditValues_copiesDescriptionDueDateAndPriority` |
| 優先度の定数名 | 外部と共有している値から変わっていない | `TaskPriorityTest.values_namesAreUnchanged` |
| 一覧と追加画面を行き来する | 画面が移り、キャンセルではタスクが増えない | 手動5 |
| 一覧と編集画面を行き来する | 画面が移り、キャンセルではタイトルが変わらない | 手動6 |
| タイトルを編集して保存する | 一覧に反映され、ステータスは変わらない | 手動7 |

## 3. ステータスの変更

### 完了日時（デシジョンテーブル）

完了日時は、変更の前と後のステータスの組み合わせで決まる。

| 変更の前 | 変更の後 | 完了日時 | 確かめるテスト |
| --- | --- | --- | --- |
| 完了以外 | 完了 | 設定する | `TaskServiceTest.changeStatus_toDone_setsCompletedAt`、`TaskApiControllerTest.updateStatus_toDone_savesCompletedAt` |
| 完了 | 完了 | 変えない | `TaskServiceTest.changeStatus_doneToDone_keepsCompletedAt` |
| 完了 | 完了以外 | 解除する | `TaskServiceTest.changeStatus_fromDone_clearsCompletedAt`、`TaskApiControllerTest.updateStatus_fromDone_clearsCompletedAt` |
| 完了以外 | 完了以外 | 設定しない | `TaskServiceTest.changeStatus_toOnHold_doesNotSetCompletedAt`、`TaskServiceTest.changeStatus_toCancelled_doesNotSetCompletedAt` |

### 送る値（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| ステータスが未指定 | 無効 | `null` | 400と項目別のエラー | `TaskApiControllerTest.updateStatus_nullStatus_returnsBadRequest`、`TaskServiceTest.changeStatus_nullStatus_throwsNullPointerException` |
| 存在しないステータス値 | 無効 | `INVALID` | 400と内部情報を含まないエラー | `TaskApiControllerTest.updateStatus_invalidStatus_returnsBadRequest` |
| ボディの形式がJSONでない | 無効 | `text/plain` | 400と内部情報を含まないエラー | `TaskApiControllerTest.updateStatus_nonJsonContentType_returnsBadRequest` |
| URLのIDが数値でない | 無効 | `abc` | 400と内部情報を含まないエラー | `TaskApiControllerTest.updateStatus_nonNumericId_returnsBadRequest` |
| 存在しないタスクのID | 無効 | `-1`（手動13は、別の画面で削除したタスクのID） | 404とIDを含まないメッセージ | `TaskApiControllerTest.updateStatus_notFound_returnsNotFound`、手動13 |

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 期限切れのタスクを完了にする | 期限の状況が「期限切れでも今日が期限でもない」になる | `TaskServiceTest.changeStatus_overdueToDone_returnsNormal` |
| 期限が過ぎた完了のタスクを未着手に戻す | 期限の状況が「期限切れ」になる | `TaskServiceTest.changeStatus_pastDueDoneToTodo_returnsOverdue` |
| 期限の状況が無いまま結果のデータを作る | 作らずに例外が発生する | `TaskEntityMapperTest.toStatusChangeResult_nullDueDateStatus_throwsNullPointerException` |
| ステータスの定数名 | 外部と共有している値から変わっていない | `TaskStatusTest.values_namesAreUnchanged` |
| 5つのステータスへ順に変える | 再読み込みせずに表示が切り替わり、再読み込みの後も変わらない | 手動2 |
| レスポンスを待つ間 | プルダウンを操作できない | 手動3 |
| 通信に失敗する | 知らせが出て、選択が元に戻る | 手動4 |

## 4. タスクの削除

### 削除するタスク（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 存在するタスク | 有効 | 2件登録したうちの1件のID | そのタスクだけが取り除かれ、204が返る | `TaskServiceTest.removeTask_deletesFromDatabase`、`TaskApiControllerTest.delete_returnsNoContent`、手動9 |
| 存在しないタスク | 無効 | `-1`（手動14は、別の画面で削除したタスクのID） | 404とIDを含まないメッセージ | `TaskServiceTest.removeTask_notFound_throwsException`、`TaskApiControllerTest.delete_notFound_returnsNotFound`、手動14 |
| URLのIDが数値でない | 無効 | `abc` | 400と内部情報を含まないエラー | `TaskApiControllerTest.delete_nonNumericId_returnsBadRequest` |

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 削除の確認でキャンセルする | 削除されない | 手動8 |
| 最後の1件を削除する | 再読み込みせずに「タスクはありません」が表示される | 手動10 |

## 5. 絞り込み・検索

### 条件の値（同値分割法）

| 条件 | 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- | --- |
| ステータス | 5つの値のそれぞれ | 有効 | 5つの値のそれぞれ | そのステータスのタスクだけが返る | `TaskServiceTest.searchTasks_byStatus_returnsOnlyThatStatus`（5つの値で1回ずつ） |
| 優先度 | 3つの値のそれぞれ | 有効 | 3つの値のそれぞれ | その優先度のタスクだけが返る | `TaskServiceTest.searchTasks_byPriority_returnsOnlyThatPriority`（3つの値で1回ずつ） |
| ステータス・期限・優先度 | 存在しない値 | 無効 | 存在しない値 | 400と内部情報を含まないエラー | `TaskApiControllerTest.search_invalidEnumValue_returnsBadRequestWithoutInternalInfo`（3つの条件で1回ずつ） |
| カテゴリ | 存在するカテゴリのID | 有効 | 存在するカテゴリのID | そのカテゴリのタスクだけが返る | `TaskServiceTest.searchTasks_byCategoryId_returnsOnlyThatCategory`、`TaskRequestMapperTest.toSearchCondition_numericCategory_returnsById`、手動18 |
| カテゴリ | `NONE`（カテゴリなし） | 有効 | `NONE` | カテゴリのないタスクだけが返る | `TaskServiceTest.searchTasks_noCategory_returnsOnlyTasksWithoutCategory`、`TaskRequestMapperTest.toSearchCondition_noneCategory_returnsNone`、手動18 |
| カテゴリ | 存在しないカテゴリのID（1〜18桁の数） | 有効 | 削除したカテゴリのID | 0件 | `TaskServiceTest.searchTasks_missingCategoryId_returnsEmpty` |
| カテゴリ | 19桁以上の数 | 無効 | 20桁の数 | 400と内部情報を含まないエラー | `TaskApiControllerTest.search_malformedCategory_returnsBadRequestWithoutInternalInfo` |
| カテゴリ | 数字でも`NONE`でもない値 | 無効 | `abc`、`none`、空文字 | 400と内部情報を含まないエラー | `TaskApiControllerTest.search_malformedCategory_returnsBadRequestWithoutInternalInfo` |
| キーワード | タイトルか詳細に含まれる文字 | 有効 | 「会議」 | 含むタスクが返る | `TaskServiceTest.searchTasks_byKeyword_matchesTitleOrDescription`、手動19 |
| キーワード | 大文字・小文字が違う文字 | 有効 | 「Report」に対する「rep」 | 区別せずに返る | `TaskServiceTest.searchTasks_byKeyword_ignoresCase` |
| キーワード | `%`・`_`・`\` を含む | 有効 | 「0%」「a_b」「\」 | ただの文字として探す | `TaskServiceTest.searchTasks_keywordWithPercent_matchesLiterally`、`TaskServiceTest.searchTasks_keywordWithUnderscore_matchesLiterally`、`TaskServiceTest.searchTasks_keywordWithBackslash_matchesLiterally` |
| キーワード | 空白（半角・全角）だけ | 有効 | 半角・全角の空白だけ | キーワードで絞り込まない | `TaskRequestMapperTest.toSearchCondition_blankKeyword_returnsNullKeyword`、手動19 |
| キーワード | 101文字以上 | 無効 | 101文字 | 400と項目別のエラー | `TaskApiControllerTest.search_keywordOf101Characters_returnsBadRequest` |

`TaskApiControllerTest.search_malformedCategory_returnsBadRequestWithoutInternalInfo` は、`abc`、`none`、空文字、19桁、20桁の5つの値で1回ずつ動く。

### 条件の値（境界値分析）

| 条件 | 境界 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| カテゴリ | 桁数の上限 | 18桁の数 | 受け付ける | `TaskApiControllerTest.search_categoryOf18Digits_returnsOk` |
| カテゴリ | 桁数の上限 | 19桁の数 | 400と内部情報を含まないエラー | `TaskApiControllerTest.search_malformedCategory_returnsBadRequestWithoutInternalInfo` |
| キーワード | 文字数の上限 | 100文字 | 受け付ける | `TaskApiControllerTest.search_keywordOf100Characters_returnsOk` |
| キーワード | 文字数の上限 | 101文字 | 400と項目別のエラー | `TaskApiControllerTest.search_keywordOf101Characters_returnsBadRequest` |

### 期限の絞り込み（デシジョンテーブル）

期限とステータスを組み合わせた8件のタスクを登録し、どれが返るかを試す。

| 期限 | ステータス | 「今日まで」で返るか | 「期限切れのみ」で返るか |
| --- | --- | --- | --- |
| なし | 未着手 | 返らない | 返らない |
| 昨日 | 未着手 | 返らない | 返る |
| 昨日 | 完了・中止 | 返らない | 返らない |
| 今日 | 未着手 | 返る | 返らない |
| 今日 | 完了・中止 | 返らない | 返らない |
| 明日 | 未着手 | 返らない | 返らない |

確かめるテスト: `TaskServiceTest.searchTasks_dueToday_returnsOpenTasksDueToday`、`TaskServiceTest.searchTasks_overdue_returnsOpenTasksPastDue`、手動17

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 条件なし | 全件が表示順の昇順、同じ場合はIDの昇順で返る | `TaskServiceTest.searchTasks_noConditions_returnsAllInListOrder`、`TaskRequestMapperTest.toSearchCondition_noConditions_returnsAllNull` |
| ステータスと優先度の組み合わせ | 両方に合うタスクだけが返る | `TaskServiceTest.searchTasks_byStatusAndPriority_returnsTasksMatchingBoth`、手動16 |
| カテゴリとステータスの組み合わせ | 両方に合うタスクだけが返る | 手動18 |
| ステータスとキーワードの組み合わせ | 両方に合うタスクだけが返る | 手動22 |
| 5つの条件すべての組み合わせ | すべての条件に合うタスクだけが返る | `TaskServiceTest.searchTasks_allConditions_returnsTasksMatchingAll` |
| APIに条件を送る | 200と、条件に合うタスクの配列が返る | `TaskApiControllerTest.search_returnsArrayOfMatchingTasks` |
| キーワードの前後に空白（半角・全角）がある | 前後の空白を除き、間の空白は残す | `TaskRequestMapperTest.toSearchCondition_keywordWithSpaces_stripsBothEnds` |
| ステータスのボタンの並び | 選ばれた「すべて」の後に、すべてのステータス | `TaskControllerTest.list_statusFilter_listsAllThenStatuses` |
| 期限のボタンの並び | 選ばれた「すべて」の後に、「今日まで」「期限切れのみ」 | `TaskControllerTest.list_dueFilter_listsAllThenDueDateConditions` |
| 優先度のボタンの並び | 選ばれた「すべて」の後に、すべての優先度 | `TaskControllerTest.list_priorityFilter_listsAllThenPriorities` |
| カテゴリのプルダウンの並び | 「すべて」、「カテゴリなし」の後に、作成した順 | `TaskControllerTest.list_categoryFilter_listsAllNoneThenCategoriesInCreatedOrder`、手動18 |
| ステータス・期限・優先度を1つずつ移す | そのまま移される | `TaskRequestMapperTest.toSearchCondition_copiesStatusDueAndPriority` |
| 一覧を開いたとき | 「条件に合うタスクはありません」と取得の失敗の文が隠れている | `TaskControllerTest.list_hidesNoMatchAndLoadErrorMessages` |
| 絞り込んだ後に行を作るテンプレート | すべてのステータスの選択肢と、編集画面のURLがある | `TaskControllerTest.list_rowTemplate_containsStatusOptionsAndEditUrl` |
| 絞り込んだ後に優先度のバッジを作るテンプレート | 優先度ごとのクラスと表示名のバッジがある | `TaskControllerTest.list_priorityBadgeTemplate_containsBadgeForEachPriority` |
| ボタンを押す | 条件に合うタスクだけが、画面を再読み込みせずに表示され、押したボタンが選ばれた見た目になる | 手動16 |
| キーワードを入力する | 入力をやめてから絞り込み、変換を確定するまでは送らない | 手動19 |
| 絞り込んでから「すべて」に戻した表と、絞り込んだ後の行 | 絵文字を含めて絞り込む前と同じ見た目になり、ステータスを変えられる | 手動20 |
| 条件に合うタスクが無い | 「条件に合うタスクはありません」が表示される | 手動21 |
| 通信に失敗する | 知らせが出て表が隠れ、条件を変えると戻る | 手動22 |
| レスポンスを待つ間に条件を変える | 前のリクエストが取り消され、最後の条件の結果が出る | 手動23 |
| 絞り込んだ一覧で削除して0件になる | 条件を選んでいるかで文言が変わる | 手動24 |
| 0件で開いた後に別の画面でタスクを足す | 次に絞り込んだときに、足したタスクに合った文と表が出る | 手動25 |

## 6. 並び替え

### 送るIDの並び（同値分割法）

送るIDの並びが、今のタスクのIDをすべて1回ずつ含む形のときだけ保存する。そうでない形は、形ごとに分ける。

「試す値」の「タスク1」「タスク2」「タスク3」は、テストで登録した順のタスクのIDを指す。`TaskServiceTest` の値を書く。

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| すべてのタスクのIDを1回ずつ含む | 有効 | タスク1〜3があるとき、タスク3・タスク1・タスク2 | 送った順に1から表示順が保存され、204が返る | `TaskServiceTest.reorderTasks_allIds_savesSortOrderFromOneInSentOrder`、`TaskApiControllerTest.reorder_allIds_returnsNoContentAndSavesSortOrder`、手動26 |
| IDが足りない | 無効 | タスク1・2があるとき、タスク2 | 例外（API では409）、表示順は変わらない | `TaskServiceTest.reorderTasks_missingId_throwsAndKeepsSortOrder`、`TaskApiControllerTest.reorder_idsNotMatchingTasks_returnsConflictAndKeepsSortOrder` |
| タスクがあるのに空の並び | 無効 | タスク1・2があるとき、空の並び | 例外、表示順は変わらない | `TaskServiceTest.reorderTasks_emptyListWithTasks_throwsAndKeepsSortOrder` |
| すべてのIDに加えて、存在しないIDがある | 無効 | タスク1・2があるとき、タスク2・タスク1・`-1`（手動30は、今あるすべてのIDに、別の画面で削除したタスクのIDが加わった並び） | 例外、表示順は変わらない | `TaskServiceTest.reorderTasks_extraId_throwsAndKeepsSortOrder`、手動30 |
| IDの数は合うが、存在しないIDがある | 無効 | タスク1・2があるとき、タスク2・`-1` | 例外、表示順は変わらない | `TaskServiceTest.reorderTasks_nonExistentIdInsteadOfTask_throwsAndKeepsSortOrder` |
| IDの数は合うが、重複したIDがある | 無効 | タスク1〜3があるとき、タスク2・タスク2・タスク1 | 例外、表示順は変わらない | `TaskServiceTest.reorderTasks_duplicateIdInsteadOfTask_throwsAndKeepsSortOrder` |
| すべてのIDに加えて、重複したIDがある | 無効 | タスク1・2があるとき、タスク2・タスク1・タスク2 | 例外、表示順は変わらない | `TaskServiceTest.reorderTasks_allIdsWithDuplicate_throwsAndKeepsSortOrder` |
| nullが入っている | 無効 | タスク1・2があるとき、タスク2・`null` | 例外（APIでは400ではなく409）、表示順は変わらない | `TaskServiceTest.reorderTasks_nullInList_throwsAndKeepsSortOrder`、`TaskApiControllerTest.reorder_nullInIds_returnsConflict` |
| `taskIds` が無い、またはnull | 無効 | `{}`、`{"taskIds":null}` | 400と項目別のエラー | `TaskApiControllerTest.reorder_noTaskIds_returnsBadRequest`（2つのボディで1回ずつ） |
| 数値でない値がある | 無効 | `{"taskIds":["abc"]}` | 400と内部情報を含まないエラー | `TaskApiControllerTest.reorder_nonNumericId_returnsBadRequest` |

### タスクの件数（境界値分析）

並び替えるタスクの件数の下限は0件。

| 境界 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- |
| 件数の下限 | タスクが0件で、空の並びを送る | 例外にならない | `TaskServiceTest.reorderTasks_noTasksAndEmptyList_doesNotThrow` |
| 件数の下限 | タスクが1件で、そのIDを送る | 表示順が1になる | `TaskServiceTest.reorderTasks_oneTask_savesSortOrderOne` |

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 並び替える | 表示順が変わったタスクだけ更新日時が変わる | `TaskServiceTest.reorderTasks_updatesUpdatedAtOnlyOfChangedTasks` |
| 表の見出し | 左端に「順」の列がある | `TaskControllerTest.list_tableHeader_startsWithHandleColumn` |
| タスクの行 | 左端に「≡」がある | `TaskControllerTest.list_taskRow_startsWithSortHandle` |
| 絞り込んだ後に行を作るテンプレート | 左端に「≡」がある | `TaskControllerTest.list_rowTemplate_startsWithSortHandle` |
| タスクがあるときに一覧を開く | 並び替えのやり方の文が出て、絞り込み中の文が隠れている | `TaskControllerTest.list_withTasks_showsSortGuideAndHidesFilterNotice` |
| タスクが0件 | 表の上の文が隠れている | `TaskControllerTest.list_noTasks_hidesSortNotice` |
| 一覧で読み込むJavaScript | jQueryの後、画面のJavaScriptの前にjQuery UIを読み込み、そのファイルを開ける | `TaskControllerTest.list_loadsJqueryUiBetweenJqueryAndListScript` |
| 一覧で読み込むCSS | 一覧の画面のCSSを読み込む | `TaskControllerTest.list_loadsListCss` |
| 「≡」以外をつかむ、同じ位置で離す | 動かず、送られない | 手動27 |
| 絞り込み中 | 「≡」が消え、文が出て、並び替えできない | 手動28 |
| 絞り込みの結果が0件 | 並び替えの文を出さない | 手動29 |
| 通信に失敗する | 知らせの後に、行が元の位置に戻る | 手動31 |
| レスポンスを待つ間 | 次の並び替えと行の操作が止まり、絞り込みの欄は操作できる | 手動32 |
| レスポンスを待つ間に条件を変える | レスポンスの後に、最後の条件で1回だけ絞り込む | 手動33 |
| ステータス変更と削除のレスポンスを待つ間 | 並び替えが止まる | 手動34 |
| 行をつかんだまま絞り込みが送られる | 行がマウスについてこなくなる | 手動35 |

## 7. カテゴリ

### 名前（同値分割法）

上限は20文字。前後の空白（全角を含む）は削ってから数える。

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 空白だけ（入力なし） | 無効 | 空文字、全角空白だけ | エラー、登録しない | `CategoryControllerTest.create_emptyName_showsErrorAndDoesNotSave`、`CategoryControllerTest.create_fullWidthSpaceName_showsErrorAndDoesNotSave` |
| 1〜20文字で、ほかのカテゴリと重ならない | 有効 | 「テストで作成したカテゴリ」 | 登録する | `CategoryControllerTest.create_validNameAndColor_savesWithLowerCaseColor` |
| 21文字以上 | 無効 | 21文字 | エラー、登録しない | `CategoryControllerTest.create_name21Chars_showsErrorAndDoesNotSave` |
| 大文字・小文字だけが違う名前のカテゴリがある | 無効 | 今あるカテゴリの名前の大文字・小文字を変えた名前 | 重複のエラー、登録しない | `CategoryControllerTest.create_nameDiffersOnlyInCase_showsDuplicateErrorAndDoesNotSave` |

### 名前（境界値分析）

| 境界 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- |
| 下限 | 0文字（空文字） | エラー、登録しない | `CategoryControllerTest.create_emptyName_showsErrorAndDoesNotSave` |
| 下限 | 1文字 | 登録する | `CategoryControllerTest.create_name1Char_saves` |
| 上限 | 20文字 | 登録する | `CategoryControllerTest.create_name20Chars_saves` |
| 上限 | 21文字 | エラー、登録しない | `CategoryControllerTest.create_name21Chars_showsErrorAndDoesNotSave` |

### 色（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| `#rrggbb` の形（大文字を含む） | 有効 | `#1E88E5` | 小文字にして登録する | `CategoryControllerTest.create_validNameAndColor_savesWithLowerCaseColor`、`CategoryFormMapperTest.toInput_noColorUnchecked_copiesNameAndColor` |
| 「色を付けない」を選ぶ | 有効 | 「色を付けない」を選び、色の欄は `#000000` | 色の欄の値は使わず、色なしで登録する | `CategoryControllerTest.create_noColorChecked_savesColorAsNull`、`CategoryFormMapperTest.toInput_noColorChecked_setsColorToNull` |
| `#rrggbb` の形でない | 無効 | `red`（「色を付けない」を選んだときも同じ） | エラー、登録しない | `CategoryControllerTest.create_invalidColor_showsErrorAndDoesNotSave`、`CategoryControllerTest.create_noColorCheckedAndInvalidColor_showsErrorAndDoesNotSave` |

### 削除するカテゴリ（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 存在するカテゴリ | 有効 | 2件登録したうちの1件のID | 取り除かれ、204が返る | `CategoryApiControllerTest.delete_returnsNoContent` |
| タスクが付いているカテゴリ | 有効 | タスクが1件付いたカテゴリのID | タスクは削除されず、カテゴリなしになる | `CategoryApiControllerTest.delete_categoryWithTask_setsTaskCategoryToNull` |
| 存在しないカテゴリ | 無効 | `-1`（手動15は、別の画面で削除したカテゴリのID） | 404とIDを含まないメッセージ | `CategoryApiControllerTest.delete_notFound_returnsNotFound`、手動15 |
| URLのIDが数値でない | 無効 | `abc` | 400と内部情報を含まないエラー | `CategoryApiControllerTest.delete_nonNumericId_returnsBadRequest` |

### カテゴリの件数（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 1件以上 | 有効 | カテゴリを1件足す | 「カテゴリはありません」が表示されない | `CategoryControllerTest.list_withCategories_hidesEmptyMessage` |
| 0件 | 有効 | カテゴリをすべて消す | 「カテゴリはありません」が表示される | `CategoryControllerTest.list_noCategories_showsEmptyMessage` |

### そのほかのテストケース

| テストケース | 期待する結果 | 確かめるテスト |
| --- | --- | --- |
| 名前の前後に空白がある | 空白を削って登録する | `CategoryControllerTest.create_nameWithSurroundingSpaces_savesStrippedName` |
| 一覧の並び | 作成した順に並ぶ | `CategoryControllerTest.list_showsCategoriesInCreatedOrder` |
| URLにカテゴリ名を付けて追加画面を開く | その名前を入力欄に入れない | `CategoryControllerTest.newForm_withQueryParam_showsEmptyForm` |

## 8. エラーページ

### 開くURL（同値分割法）

| 同値パーティション | 有効・無効 | 試す値 | 期待する結果 | 確かめるテスト |
| --- | --- | --- | --- | --- |
| 存在しないURL | 無効 | `/no-such-page` | 404でエラーページが出て、エラーの番号は出ず、タスク一覧へ戻れる | `ErrorPageTest.open_noSuchPage_showsErrorPageWith404`、手動36 |
| IDが数値でない編集のURL | 無効 | `/tasks/abc/edit` | 400でエラーページが出て、エラーの番号は出ず、タスク一覧へ戻れる | `ErrorPageTest.open_nonNumericTaskId_showsErrorPageWith400`、手動37 |
| 想定外の例外が起きるURL | 無効 | 一覧の取得で例外を起こし、`/tasks` を開く | 500でエラーページが出て、エラーの番号は出ない | `ErrorPageTest.open_unexpectedException_showsErrorPageWith500` |
| APIのURLでエラーが起きる | 無効 | PATCHだけを受ける `/api/tasks/1/status` を、GETで開く | エラーページではなくJSONが返る | `ErrorPageTest.open_apiWithUnsupportedMethod_returnsJson` |
