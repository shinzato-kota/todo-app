package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.DueDateCondition;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.repository.task.TaskRepository;
import com.shinzato.todoapp.service.task.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskController}のテスト。
 *
 * <p>
 * 実際にリクエストを送って画面のレスポンスを確認する。
 *
 * <p>
 * URLは実際のアクセスと同じ形にするため、コンテキストパスを含めて指定する。
 *
 * <p>
 * 開発用のデータベースに接続して確認するため、テストでの変更が残らないよう@Transactionalを付けている。
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestEntityManager
@Transactional
class TaskControllerTest {

    /** 並び順を確かめないテストで使う表示順。すべて同じ値にして、一覧の並びをIDの順（登録した順）にする */
    private static final int SORT_ORDER_NOT_CHECKED = 1;

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** 期限の状況を確かめるテストで、アプリと同じ「今日」を求めるために使う */
    @Autowired
    private Clock clock;

    /** 設定値と同じコンテキストパスを使うため、プロパティから読み込む */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Nested
    @DisplayName("一覧の画面（GET /tasks）")
    class ListPage {
        @Test
        @DisplayName("保留・中止のタスクは、表示スタイルを切り替えるためのクラスがタスクの行とステータスのプルダウンに付与される")
        void list_addsStatusClassToRowAndStatusSelect() {
            // 準備: 保留は、列挙型の値の「_」がクラス名で「-」になることも確かめるために使う
            Task onHold = createTask("保留にするタスク", TaskStatus.ON_HOLD);
            Task cancelled = createTask("中止にするタスク", TaskStatus.CANCELLED);

            // 実行・確認: 開発用のデータにも同じステータスのタスクがありうるため、IDで行を取り出して確認する
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        assertThat(extractRow(html, onHold.getId()))
                                .contains(
                                        "class=\"task-table-row task-table-row--status_on-hold task-table-row--due_normal\"")
                                .contains("class=\"status-select js-status-select status-select--status_on-hold\"");
                        assertThat(extractRow(html, cancelled.getId()))
                                .contains(
                                        "class=\"task-table-row task-table-row--status_cancelled task-table-row--due_normal\"")
                                .contains("class=\"status-select js-status-select status-select--status_cancelled\"");
                    });
        }

        @Test
        @DisplayName("一覧に詳細・優先度・期限が表示される")
        void list_showsDescriptionPriorityAndDueDate() {
            // 準備: 優先度はエンティティの初期値(中)と区別できるよう、高にする
            Task task = new Task();
            task.setTitle("一覧に出すタスク");
            task.setDescription("1行目\n2行目");
            task.setPriority(TaskPriority.HIGH);
            task.setDueDate(LocalDate.of(2026, 10, 31));
            task.setSortOrder(SORT_ORDER_NOT_CHECKED);
            Task saved = entityManager.persistFlushFind(task);

            // 実行・確認: 詳細は改行を含めたまま出力し、期限は「/」区切りで出力する
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, saved.getId()))
                            .contains(">1行目\n2行目</p>")
                            .contains("class=\"priority-badge priority-badge--level_high\"")
                            .contains(">高</span>")
                            .contains(">2026/10/31</span>"));
        }

        @Test
        @DisplayName("期限が昨日の未着手のタスクは、タスクの行に期限切れのクラスが付き、「期限切れ」ラベルが出力される")
        void list_overdueTask_addsOverdueClassAndLabel() {
            // 準備
            Task task = createTask("期限切れのタスク", LocalDate.now(clock).minusDays(1), TaskStatus.TODO);

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, task.getId()))
                            .contains(
                                    "class=\"task-table-row task-table-row--status_todo task-table-row--due_overdue\"")
                            .contains("class=\"task-table-row__overdue-label\""));
        }

        @Test
        @DisplayName("期限が今日の未着手のタスクは、タスクの行に今日が期限のクラスが付く")
        void list_dueTodayTask_addsDueTodayClass() {
            // 準備
            Task task = createTask("今日が期限のタスク", LocalDate.now(clock), TaskStatus.TODO);

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, task.getId()))
                            .contains(
                                    "class=\"task-table-row task-table-row--status_todo task-table-row--due_due-today\""));
        }

        @Test
        @DisplayName("詳細・期限がないタスクは、詳細と期限の要素を出力しない")
        void list_noDescriptionAndDueDate_omitsElements() {
            // 準備: 詳細・期限はどちらも未設定(null)のまま登録する
            Task task = createTask("詳細・期限なしのタスク");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, task.getId()))
                            .doesNotContain("class=\"task-table-row__description\"")
                            .doesNotContain("class=\"task-table-row__due-date\"")
                            .doesNotContain("class=\"task-table-row__overdue-label\""));
        }

        @Test
        @DisplayName("タスクがあるときは「タスクはありません」が表示されない")
        void list_withTasks_hidesEmptyMessage() {
            // 準備
            createTask("一覧に出すタスク");

            // 実行・確認: メッセージは件数に関係なくHTMLに含まれ、hidden属性で表示を切り替えているため、属性を確認する
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .contains("class=\"empty-message task-list__empty-message js-empty-message\" hidden=\"hidden\"")
                    .doesNotContain("class=\"data-table task-list__table js-task-table\" hidden=\"hidden\"");
        }

        @Test
        @DisplayName("タスクが0件のときは「タスクはありません」が表示される")
        void list_noTasks_showsEmptyMessage() {
            // 準備: このテストのトランザクション内で全件削除する(終了時にロールバックされる)
            taskRepository.deleteAll();

            // 実行・確認: メッセージは件数に関係なくHTMLに含まれ、hidden属性で表示を切り替えているため、属性を確認する
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .doesNotContain(
                            "class=\"empty-message task-list__empty-message js-empty-message\" hidden=\"hidden\"")
                    .contains("class=\"data-table task-list__table js-task-table\" hidden=\"hidden\"");
        }

        @Test
        @DisplayName("一覧を開いたときは、「条件に合うタスクはありません」と、取得できなかったことを知らせる文が隠れている")
        void list_hidesNoMatchAndLoadErrorMessages() {
            // 準備: タスクがある場合も表の代わりに出ないことを確かめるため、1件登録する
            createTask("一覧に出すタスク");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        assertThat(extractStartTag(html, "js-no-match-message")).contains("hidden");
                        assertThat(extractStartTag(html, "js-load-error-message")).contains("hidden");
                    });
        }

        @Test
        @DisplayName("知らせる文を渡されて一覧を開くと、その文がJavaScriptへ渡すための属性に出力される")
        void list_withAlertMessage_outputsAlertMessageAttribute() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath)
                    .flashAttr("alertMessage", "タスクが見つかりません。"))
                    .hasStatusOk()
                    .bodyText()
                    .contains("data-alert-message=\"タスクが見つかりません。\"");
        }

        @Test
        @DisplayName("知らせる文がなく一覧を開くと、JavaScriptへ渡すための属性を出力しない")
        void list_withoutAlertMessage_omitsAlertMessageAttribute() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .doesNotContain("data-alert-message");
        }

        @Test
        @DisplayName("色のあるカテゴリのタスクは、カテゴリの列に、その色の印と名前が出る")
        void list_categoryWithColor_showsColorMarkAndName() {
            // 準備
            String categoryName = "色のあるカテゴリ";
            String categoryColor = "#1e88e5";
            Category category = createCategory(categoryName, categoryColor);
            Task task = createTask("色のあるカテゴリのタスク", category);

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, task.getId()))
                            .contains("class=\"category-label\"")
                            .contains("style=\"color: " + categoryColor + "\"")
                            .contains(">" + categoryName + "<"));
        }

        @Test
        @DisplayName("色なしのカテゴリのタスクは、カテゴリの列に、印を見えなくするクラスと名前が出る")
        void list_categoryWithoutColor_showsNameWithHiddenMark() {
            // 準備
            String categoryName = "色なしのカテゴリ";
            Category category = createCategory(categoryName);
            Task task = createTask("色なしのカテゴリのタスク", category);

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, task.getId()))
                            .contains("class=\"category-label category-label--no-color\"")
                            .contains(">" + categoryName + "<")
                            .doesNotContain("style=\"color:"));
        }

        @Test
        @DisplayName("カテゴリなしのタスクは、カテゴリの列に何も出さない")
        void list_noCategory_omitsCategoryLabel() {
            // 準備
            Task task = createTask("カテゴリなしのタスク");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractRow(html, task.getId()))
                            .doesNotContain("category-label"));
        }

        @Test
        @DisplayName("ステータスの絞り込みは、選ばれた「すべて」の後に、すべてのステータスが選ばれていない状態で並ぶ")
        void list_statusFilter_listsAllThenStatuses() {
            // 準備
            List<String> values = List.of(TaskStatus.TODO.name(), TaskStatus.IN_PROGRESS.name(),
                    TaskStatus.ON_HOLD.name(), TaskStatus.DONE.name(), TaskStatus.CANCELLED.name());
            List<String> names = List.of(TaskStatus.TODO.getDisplayName(), TaskStatus.IN_PROGRESS.getDisplayName(),
                    TaskStatus.ON_HOLD.getDisplayName(), TaskStatus.DONE.getDisplayName(),
                    TaskStatus.CANCELLED.getDisplayName());

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertFilterButtons(extractFilterGroup(html, "status"), values, names));
        }

        @Test
        @DisplayName("期限の絞り込みは、選ばれた「すべて」の後に、「今日まで」「期限切れのみ」が選ばれていない状態で並ぶ")
        void list_dueFilter_listsAllThenDueDateConditions() {
            // 準備: ボタンの値は、APIが受け取る期限の条件の値と同じにする
            List<String> values = List.of(DueDateCondition.DUE_TODAY.name(), DueDateCondition.OVERDUE.name());
            List<String> names = List.of("今日まで", "期限切れのみ");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertFilterButtons(extractFilterGroup(html, "due"), values, names));
        }

        @Test
        @DisplayName("優先度の絞り込みは、選ばれた「すべて」の後に、すべての優先度が選ばれていない状態で並ぶ")
        void list_priorityFilter_listsAllThenPriorities() {
            // 準備
            List<String> values = List.of(TaskPriority.HIGH.name(), TaskPriority.MEDIUM.name(),
                    TaskPriority.LOW.name());
            List<String> names = List.of(TaskPriority.HIGH.getDisplayName(), TaskPriority.MEDIUM.getDisplayName(),
                    TaskPriority.LOW.getDisplayName());

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertFilterButtons(extractFilterGroup(html, "priority"), values, names));
        }

        @Test
        @DisplayName("カテゴリの絞り込みは、選ばれた「すべて」、「カテゴリなし」の後に、カテゴリが作成した順に並ぶ")
        void list_categoryFilter_listsAllNoneThenCategoriesInCreatedOrder() {
            // 準備: 名前の順で並べた場合と結果が変わるよう、「Zで始まるカテゴリ」「Aで始まるカテゴリ」の順で作成する
            Category createdFirst = createCategory("Zで始まるカテゴリ");
            Category createdSecond = createCategory("Aで始まるカテゴリ");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        String select = extractFilterCategorySelect(html);
                        int all = select.indexOf("<option value=\"\" selected>すべて</option>");
                        int none = select.indexOf("<option value=\"NONE\">カテゴリなし</option>");
                        int first = select.indexOf("value=\"" + createdFirst.getId() + "\"");
                        int second = select.indexOf("value=\"" + createdSecond.getId() + "\"");
                        assertThat(all).isNotNegative().isEqualTo(select.indexOf("<option"));
                        assertThat(none).isGreaterThan(all);
                        assertThat(first).isGreaterThan(none);
                        assertThat(second).isGreaterThan(first);
                        assertThat(select)
                                .contains(">" + createdFirst.getName() + "</option>")
                                .contains(">" + createdSecond.getName() + "</option>");
                        // 「すべて」のほかに選ばれた選択肢がないことを、selected属性の数で確かめる
                        assertThat(StringUtils.countOccurrencesOf(select, "selected")).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("絞り込んだ後にJavaScriptがタスクの行を作るためのテンプレート（template要素）には、すべてのステータスの選択肢と、IDを足す前の編集画面のURLがある")
        void list_rowTemplate_containsStatusOptionsAndEditUrl() {
            // 実行・確認: 選ぶ選択肢はJavaScriptが値で決めるため、テンプレートではselected属性を付けない
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        String template = extractTemplate(html, "js-task-row-template");
                        int previous = -1;
                        for (TaskStatus status : TaskStatus.values()) {
                            int value = template.indexOf("value=\"" + status.name() + "\"");
                            assertThat(value).as("%s の選択肢", status).isGreaterThan(previous);
                            assertThat(template.indexOf(">" + status.getDisplayName() + "</option>", value))
                                    .as("%s の表示名", status)
                                    .isNotNegative();
                            previous = value;
                        }
                        assertThat(template)
                                .doesNotContain("selected")
                                .contains("href=\"" + contextPath + "/tasks\"");
                    });
        }

        @Test
        @DisplayName("絞り込んだ後にJavaScriptが優先度のバッジを作るためのテンプレート（template要素）には、優先度ごとに、その優先度のクラスと表示名のバッジがある")
        void list_priorityBadgeTemplate_containsBadgeForEachPriority() {
            // 実行・確認: JavaScriptは列挙型の値でバッジを探すため、値・クラス・表示名の組を確かめる
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        String template = extractTemplate(html, "js-priority-badge-template");
                        assertThat(extractBadge(template, TaskPriority.HIGH))
                                .contains("class=\"priority-badge js-priority-badge priority-badge--level_high\"")
                                .contains(">" + TaskPriority.HIGH.getDisplayName() + "</span>");
                        assertThat(extractBadge(template, TaskPriority.MEDIUM))
                                .contains("class=\"priority-badge js-priority-badge priority-badge--level_medium\"")
                                .contains(">" + TaskPriority.MEDIUM.getDisplayName() + "</span>");
                        assertThat(extractBadge(template, TaskPriority.LOW))
                                .contains("class=\"priority-badge js-priority-badge priority-badge--level_low\"")
                                .contains(">" + TaskPriority.LOW.getDisplayName() + "</span>");
                    });
        }

        @Test
        @DisplayName("一覧の表の左端に、見出しが「順」の列がある")
        void list_tableHeader_startsWithHandleColumn() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        int table = html.indexOf("js-task-table");
                        int handleHeader = html.indexOf("task-list__handle-cell", table);
                        int headerEnd = html.indexOf("</th>", handleHeader);
                        assertThat(handleHeader).isNotNegative();
                        assertThat(html.substring(handleHeader, headerEnd)).contains("順");
                        assertThat(html.indexOf("</th>", table)).isGreaterThan(handleHeader);
                        assertThat(html.indexOf("タイトル</th>", table)).isGreaterThan(headerEnd);
                    });
        }

        @Test
        @DisplayName("タスクの行の左端に、ドラッグでつかむ「≡」がある")
        void list_taskRow_startsWithSortHandle() {
            // 準備
            Task task = createTask("並び替えるタスク");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        String row = extractRow(html, task.getId());
                        assertThat(row).contains("≡");
                        assertThat(row.indexOf("js-sort-handle"))
                                .isNotNegative()
                                .isLessThan(row.indexOf("js-task-title"));
                    });
        }

        @Test
        @DisplayName("絞り込んだ後にJavaScriptがタスクの行を作るためのテンプレート（template要素）にも、タスクの行の左端に「≡」がある")
        void list_rowTemplate_startsWithSortHandle() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        String template = extractTemplate(html, "js-task-row-template");
                        assertThat(template).contains("≡");
                        assertThat(template.indexOf("js-sort-handle"))
                                .isNotNegative()
                                .isLessThan(template.indexOf("js-task-title"));
                    });
        }

        @Test
        @DisplayName("タスクがあるときに一覧を開くと、表の上に並び替えのやり方の文が出て、絞り込み中の文が隠れている")
        void list_withTasks_showsSortGuideAndHidesFilterNotice() {
            // 準備
            createTask("一覧に出すタスク");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        assertThat(extractStartTag(html, "js-sort-notice")).doesNotContain("hidden");
                        assertThat(extractStartTag(html, "js-sort-guide")).doesNotContain("hidden");
                        assertThat(extractStartTag(html, "js-filter-notice")).contains("hidden");
                        assertThat(html).contains("≡をつかんで動かすと、並び替えられます");
                        assertThat(html).contains("絞り込み中は並び替えできません");
                    });
        }

        @Test
        @DisplayName("タスクが0件のときは、表の上の文が隠れている")
        void list_noTasks_hidesSortNotice() {
            // 準備: このテストのトランザクション内で全件削除する(終了時にロールバックされる)
            taskRepository.deleteAll();

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> assertThat(extractStartTag(html, "js-sort-notice")).contains("hidden"));
        }

        @Test
        @DisplayName("一覧は、jQueryの後、画面のJavaScriptの前にjQuery UIを読み込み、そのファイルを開ける")
        void list_loadsJqueryUiBetweenJqueryAndListScript() {
            // 実行・確認: jQuery UIはjQueryを使い、画面のJavaScriptはjQuery UIを使うため、読み込む順を確かめる
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .satisfies(html -> {
                        int jqueryUi = html.indexOf(contextPath + "/webjars/jquery-ui/");
                        assertThat(html.indexOf(contextPath + "/webjars/jquery/"))
                                .isNotNegative()
                                .isLessThan(jqueryUi);
                        assertThat(html.indexOf(contextPath + "/js/list.js")).isGreaterThan(jqueryUi);
                    });

            assertThat(mockMvc.get().uri(contextPath + "/webjars/jquery-ui/jquery-ui.min.js").contextPath(contextPath))
                    .hasStatusOk();
        }

        @Test
        @DisplayName("一覧は、一覧の画面のCSSを読み込む")
        void list_loadsListCss() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                    .hasStatusOk()
                    .bodyText()
                    .contains("href=\"" + contextPath + "/css/list.css\"");
        }

        /**
         * 絞り込みの1つの条件のボタンが、選ばれた「すべて」の後に、選ばれていない選択肢の順で並ぶことを確かめる。
         *
         * @param group  {@link #extractFilterGroup(String, String)}で取り出した、1つの条件のボタンのHTML
         * @param values 「すべて」の後に並ぶボタンの値。並ぶ順
         * @param names  「すべて」の後に並ぶボタンの文字。並ぶ順
         */
        private void assertFilterButtons(String group, List<String> values, List<String> names) {
            assertThat(extractStartTag(group, "data-value=\"\""))
                    .contains("filter-button--selected")
                    .contains("aria-pressed=\"true\"");
            int previous = group.indexOf("data-value=\"\"");
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                int index = group.indexOf("data-value=\"" + value + "\"");
                assertThat(index).as("%s のボタン", value).isGreaterThan(previous);
                assertThat(extractStartTag(group, "data-value=\"" + value + "\""))
                        .doesNotContain("filter-button--selected")
                        .contains("aria-pressed=\"false\"");
                assertThat(group.indexOf(names.get(i), index)).as("%s の文字", value).isNotNegative();
                previous = index;
            }
            // 「すべて」と、渡した選択肢のほかにボタンがないことを、ボタンの数で確かめる
            assertThat(StringUtils.countOccurrencesOf(group, "<button")).isEqualTo(values.size() + 1);
        }

        /**
         * 一覧画面のHTMLから、指定したタスクの行を取り出す。
         *
         * <p>
         * 開発用のデータのほかの行と区別して確かめるために使う。
         *
         * @param html 一覧画面のHTML
         * @param id   取り出すタスクのID
         * @return 行の開始タグから、行の終わりまでのHTML
         */
        private String extractRow(String html, Long id) {
            int idIndex = html.indexOf("data-task-id=\"" + id + "\"");
            assertThat(idIndex).as("タスクID %d の行", id).isNotNegative();
            int start = html.lastIndexOf("<tr", idIndex);
            int end = html.indexOf("</tr>", start);
            return html.substring(start, end);
        }

        /**
         * 一覧画面のHTMLから、絞り込みの1つの条件のボタンの並びを取り出す。
         *
         * <p>
         * ほかの条件のボタンと区別して確かめるために使う。
         *
         * @param html      一覧画面のHTML
         * @param condition 条件の名前(status、due、priority)
         * @return 条件の行の開始タグの中から、行の終わりまでのHTML
         */
        private String extractFilterGroup(String html, String condition) {
            int start = html.indexOf("data-condition=\"" + condition + "\"");
            assertThat(start).as("%s の絞り込みの行", condition).isNotNegative();
            int end = html.indexOf("</div>", start);
            return html.substring(start, end);
        }

        /**
         * 一覧画面のHTMLから、絞り込みのカテゴリのプルダウンを取り出す。
         *
         * @param html 一覧画面のHTML
         * @return カテゴリのプルダウンの開始タグの中から、終了タグの前までのHTML
         */
        private String extractFilterCategorySelect(String html) {
            int start = html.indexOf("id=\"category\"");
            assertThat(start).as("絞り込みのカテゴリのプルダウン").isNotNegative();
            int end = html.indexOf("</select>", start);
            return html.substring(start, end);
        }

        /**
         * 一覧画面のHTMLから、JavaScriptが複製するテンプレート（{@code <template>}）を取り出す。
         *
         * <p>
         * 表の行など、テンプレートの外にある同じ形のHTMLと区別して確かめるために使う。
         *
         * @param html          一覧画面のHTML
         * @param templateClass テンプレート（{@code <template>}）に付けたクラス
         * @return {@code <template>}の開始タグの中から、終了タグの前までのHTML
         */
        private String extractTemplate(String html, String templateClass) {
            int start = html.indexOf("class=\"" + templateClass + "\"");
            assertThat(start).as("テンプレート %s", templateClass).isNotNegative();
            int end = html.indexOf("</template>", start);
            return html.substring(start, end);
        }

        /**
         * 優先度のバッジのテンプレートから、指定した優先度のバッジを取り出す。
         *
         * @param template {@link #extractTemplate(String, String)}で取り出した、優先度のバッジのテンプレート
         * @param priority 取り出すバッジの優先度
         * @return バッジの開始タグから、終了タグまでのHTML
         */
        private String extractBadge(String template, TaskPriority priority) {
            int index = template.indexOf("data-priority=\"" + priority.name() + "\"");
            assertThat(index).as("%s のバッジ", priority).isNotNegative();
            int start = template.lastIndexOf("<span", index);
            int end = template.indexOf("</span>", index) + "</span>".length();
            return template.substring(start, end);
        }

        /**
         * HTMLから、指定した文字列を含む開始タグを取り出す。
         *
         * <p>
         * 属性の間の改行や空白に左右されずに、1つの要素の属性を確かめるために使う。
         *
         * @param html   探すHTML
         * @param marker 開始タグの中にある文字列
         * @return 最初に見つかった開始タグの「&lt;」から「&gt;」までのHTML
         */
        private String extractStartTag(String html, String marker) {
            int index = html.indexOf(marker);
            assertThat(index).as("%s を含むタグ", marker).isNotNegative();
            int start = html.lastIndexOf("<", index);
            int end = html.indexOf(">", index) + 1;
            return html.substring(start, end);
        }
    }

    @Nested
    @DisplayName("追加の画面（GET /tasks/new）")
    class NewForm {
        @Test
        @DisplayName("追加画面を開くと、優先度は初めから「中」が選ばれている")
        void newForm_selectsMediumPriority() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks/new").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .contains("value=\"MEDIUM\" selected=\"selected\"")
                    .doesNotContain("value=\"HIGH\" selected=\"selected\"", "value=\"LOW\" selected=\"selected\"");
        }

        @Test
        @DisplayName("追加画面では、カテゴリのプルダウンのどの選択肢にもselected属性がなく、先頭が「カテゴリなし」である")
        void newForm_selectsNoCategoryAtTop() {
            // 準備: 「カテゴリなし」が先頭にあることを確かめるため、後ろに並ぶカテゴリを作成する
            String categoryName = "後ろに並ぶカテゴリ";
            createCategory(categoryName);

            // 実行・確認: 「カテゴリなし」を選ぶのはブラウザーで、サーバーはselected属性を付けないため、ブラウザーが先頭を選ぶ形になっていることを確かめる。
            // 優先度のプルダウンにも選ばれた選択肢があるため、カテゴリのプルダウンだけを取り出して確かめる
            assertThat(mockMvc.get().uri(contextPath + "/tasks/new").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .satisfies(html -> {
                        String select = extractCategorySelect(html);
                        assertThat(select)
                                .contains(categoryName)
                                .doesNotContain("selected");
                        assertThat(select.indexOf("<option"))
                                .isNotNegative()
                                .isEqualTo(select.indexOf("<option value=\"\">カテゴリなし</option>"));
                    });
        }

        @Test
        @DisplayName("追加画面では、カテゴリが作成した順に並ぶ")
        void newForm_listsCategoriesInCreatedOrder() {
            // 準備: 名前の順で並べた場合と結果が変わるよう、「Zで始まるカテゴリ」「Aで始まるカテゴリ」の順で作成する
            String createdFirst = "Zで始まるカテゴリ";
            String createdSecond = "Aで始まるカテゴリ";
            createCategory(createdFirst);
            createCategory(createdSecond);

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks/new").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .satisfies(html -> {
                        String select = extractCategorySelect(html);
                        assertThat(select.indexOf(createdFirst))
                                .isNotNegative()
                                .isLessThan(select.indexOf(createdSecond));
                    });
        }
    }

    @Nested
    @DisplayName("編集の画面（GET /tasks/{id}/edit）")
    class EditForm {
        @Test
        @DisplayName("編集画面を開くと、登録済みの詳細・期限・優先度が表示される")
        void editForm_existingTask_showsDescriptionDueDateAndPriority() {
            // 準備: 優先度はエンティティの初期値(中)と区別できるよう、高にする
            Task task = new Task();
            task.setTitle("編集するタスク");
            task.setDescription("1行目\n2行目");
            task.setDueDate(LocalDate.of(2026, 10, 31));
            task.setPriority(TaskPriority.HIGH);
            task.setSortOrder(SORT_ORDER_NOT_CHECKED);
            Task saved = entityManager.persistFlushFind(task);

            // 実行・確認: 期限は、日付の入力欄が受け付けるyyyy-MM-ddの形で出力される
            assertThat(mockMvc.get().uri(contextPath + "/tasks/" + saved.getId() + "/edit").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .contains(">1行目\n2行目</textarea>")
                    .contains("value=\"2026-10-31\"")
                    .contains("value=\"HIGH\" selected=\"selected\"")
                    .doesNotContain("value=\"MEDIUM\" selected=\"selected\"");
        }

        @Test
        @DisplayName("編集画面を開くと、登録済みのタイトルが表示される")
        void editForm_existingTask_showsTitle() {
            // 準備
            Task task = createTask("編集するタスク");

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks/" + task.getId() + "/edit").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .contains("タスク編集")
                    .contains("action=\"" + contextPath + "/tasks/" + task.getId() + "\"")
                    .contains("value=\"編集するタスク\"");
        }

        @Test
        @DisplayName("存在しないタスクの編集画面を開くと、一覧へ戻され、タスクが見つからないことを知らせる文が渡される")
        void editForm_notFound_redirectsToListWithAlertMessage() {
            assertThat(mockMvc.get().uri(contextPath + "/tasks/-1/edit").contextPath(contextPath))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks")
                    .flash().containsEntry("alertMessage", "タスクが見つかりません。");
        }

        @Test
        @DisplayName("編集画面では、保存済みのカテゴリが選ばれている")
        void editForm_taskWithCategory_selectsSavedCategory() {
            // 準備
            Category category = createCategory("保存済みのカテゴリ");
            Task task = createTask("カテゴリのあるタスク", category);

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/tasks/" + task.getId() + "/edit").contextPath(contextPath))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .satisfies(html -> assertThat(extractCategorySelect(html))
                            .contains("value=\"" + category.getId() + "\" selected=\"selected\""));
        }
    }

    @Nested
    @DisplayName("追加の保存（POST /tasks）")
    class Create {
        @Test
        @DisplayName("タイトルが空のまま保存しようとするとエラーになり、保存されない")
        void create_emptyTitle_showsErrorAndDoesNotSave() {
            // 準備: 開発用のデータが残っていても判定できるよう、保存前の件数を控える
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "")
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("タイトルを入力し、期限を送らずに保存すると、一覧へ遷移して期限なしで登録される")
        void create_validTitle_savesAndRedirects() {
            // 準備
            String title = "テストで登録したタスク";
            long before = taskRepository.count();

            // 実行・確認: リダイレクト先はコンテキストパスを含む形で返る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            List<TaskSummary> tasks = taskService.getTaskList();
            assertThat(tasks).hasSize((int) before + 1);
            assertThat(tasks.get(tasks.size() - 1).title()).isEqualTo(title);
            assertThat(tasks.get(tasks.size() - 1).dueDate()).isNull();
        }

        @Test
        @DisplayName("タイトルが半角スペースだけの場合もエラーになり、保存されない")
        void create_blankTitle_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "   ")
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("タイトルが全角スペースだけの場合もエラーになり、保存されない")
        void create_fullWidthSpaceTitle_showsErrorAndDoesNotSave() {
            // 準備: 全角スペースは見た目で半角と区別できないため、文字コードで書く
            String title = "\u3000\u3000\u3000";
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("タイトルが1文字なら保存できる")
        void create_title1Char_savesAndRedirects() {
            // 準備
            String title = "あ";
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            List<TaskSummary> tasks = taskService.getTaskList();
            assertThat(tasks).hasSize((int) before + 1);
            assertThat(tasks.get(tasks.size() - 1).title()).isEqualTo(title);
        }

        @Test
        @DisplayName("タイトルが100文字ちょうどなら保存できる")
        void create_title100Chars_savesAndRedirects() {
            // 準備: 全角文字でもバイト数ではなく文字数で数えることを確かめるため、日本語で作る
            String title = "あ".repeat(100);
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            List<TaskSummary> tasks = taskService.getTaskList();
            assertThat(tasks).hasSize((int) before + 1);
            assertThat(tasks.get(tasks.size() - 1).title()).isEqualTo(title);
        }

        @Test
        @DisplayName("タイトルが101文字だとエラーになり、保存されない")
        void create_title101Chars_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "あ".repeat(101))
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("100文字以内で入力してください");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("詳細・期限・優先度を入力して保存すると、その値で登録される")
        void create_withDescriptionDueDateAndPriority_savesValues() {
            // 実行・確認: 期限はブラウザーの日付欄が送る形、詳細の改行はブラウザーが送信時に変えるCR+LFで送る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "詳細・期限・優先度つきのタスク")
                    .param("description", "1行目\r\n2行目")
                    .param("dueDate", "2026-10-31")
                    .param("priority", "HIGH"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認: 新しく登録したタスクは表示順が最も大きくなるため、その1件を読み直す
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getDescription()).isEqualTo("1行目\n2行目");
            assertThat(registered.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 31));
            assertThat(registered.getPriority()).isEqualTo(TaskPriority.HIGH);
        }

        @Test
        @DisplayName("詳細に改行と絵文字を含めて保存すると、絵文字がそのまま登録される")
        void create_descriptionWithLineBreakAndEmoji_savesAsIs() {
            // 実行・確認: 詳細の改行はブラウザーが送信時に変えるCR+LFで送る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "絵文字を含む詳細のタスク")
                    .param("description", "1行目📝\r\n2行目🎉")
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認: 新しく登録したタスクは表示順が最も大きくなるため、その1件を読み直す
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getDescription()).isEqualTo("1行目📝\n2行目🎉");
        }

        @Test
        @DisplayName("詳細が改行を含めて2000文字なら、送信時に2001文字になっても保存できる")
        void create_description2000CharsWithLineBreak_saves() {
            // 準備: ブラウザーが改行を1文字と数えて2000文字になる詳細を、送信時の形(改行がCR+LF)で作る
            String description = "あ".repeat(1000) + "\r\n" + "あ".repeat(999);

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "改行を含む詳細のタスク")
                    .param("description", description)
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認: 改行が1文字になり、ちょうど2000文字で保存される
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getDescription()).hasSize(2000);
        }

        @Test
        @DisplayName("詳細が2001文字だとエラーになり、保存されない")
        void create_description2001Chars_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "詳細が長すぎるタスク")
                    .param("description", "あ".repeat(2001))
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("2000文字以内で入力してください");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("期限を空にして保存すると、期限なしで登録される")
        void create_emptyDueDate_savesWithoutDueDate() {
            // 準備
            String title = "期限が空のタスク";

            // 実行・確認: 日付欄を空にしたまま保存すると、ブラウザーは空の値を送る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("dueDate", "")
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getTitle()).isEqualTo(title);
            assertThat(registered.getDueDate()).isNull();
        }

        @Test
        @DisplayName("期限が日付でないとエラーになり、保存されない")
        void create_invalidDueDate_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認: 送信内容を書き換えた場合を想定し、日付として読めない値を送る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "期限が不正なタスク")
                    .param("dueDate", "明日")
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("入力値が不正です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("優先度が存在しない値だとエラーになり、保存されない")
        void create_unknownPriority_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認: 送信内容を書き換えた場合を想定し、選択肢にない値を送る。
            // エラーで戻った画面でも優先度を選び直せるよう、選択肢が表示されていることも確かめる
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "優先度が不正なタスク")
                    .param("priority", "URGENT"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("入力値が不正です")
                    .contains(">高</option>", ">中</option>", ">低</option>");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("優先度が空だとエラーになり、保存されない")
        void create_emptyPriority_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認: プルダウンに空の選択肢はないため、送信内容を書き換えた場合を想定する
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "優先度が空のタスク")
                    .param("priority", ""))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("優先度の項目がないとエラーになり、保存されない")
        void create_missingPriority_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認: 空で送った場合と同じく、画面からの未設定は「中」とみなさずエラーにする
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "優先度の項目がないタスク"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("必須入力です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("カテゴリを選んで保存すると、そのカテゴリで登録される")
        void create_withCategory_savesCategory() {
            // 準備
            Category category = createCategory("登録で選ぶカテゴリ");

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "カテゴリを選んだタスク")
                    .param("priority", "MEDIUM")
                    .param("categoryId", category.getId().toString()))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getCategory().getId()).isEqualTo(category.getId());
        }

        @Test
        @DisplayName("「カテゴリなし」（空の値）で保存すると、カテゴリなしで登録される")
        void create_emptyCategory_savesWithoutCategory() {
            // 準備
            String title = "カテゴリなしのタスク";

            // 実行・確認: 「カテゴリなし」の選択肢は空の値を送る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM")
                    .param("categoryId", ""))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getTitle()).isEqualTo(title);
            assertThat(registered.getCategory()).isNull();
        }

        @Test
        @DisplayName("カテゴリの項目を送らずに保存すると、カテゴリなしで登録される")
        void create_missingCategory_savesWithoutCategory() {
            // 準備
            String title = "カテゴリの項目がないタスク";

            // 実行・確認: 送信内容を書き換えた場合を想定し、カテゴリの項目を送らない
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getTitle()).isEqualTo(title);
            assertThat(registered.getCategory()).isNull();
        }

        @Test
        @DisplayName("存在しないカテゴリで保存すると、カテゴリの欄に文言が出て保存されず、選択肢はその時点のカテゴリで出る")
        void create_deletedCategory_showsErrorAndDoesNotSave() {
            // 準備: 画面を開いた後で、別の操作でカテゴリが削除された場合を想定する
            String deletedName = "削除されたカテゴリ";
            String remainingName = "残っているカテゴリ";
            Long deletedId = createDeletedCategoryId(deletedName);
            createCategory(remainingName);
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "削除されたカテゴリを選んだタスク")
                    .param("priority", "MEDIUM")
                    .param("categoryId", deletedId.toString()))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .contains("選択したカテゴリは見つかりません。選び直してください")
                    .satisfies(html -> assertThat(extractCategorySelect(html))
                            .contains(remainingName)
                            .doesNotContain(deletedName));

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("存在しないカテゴリで、タイトルも空にして保存すると、2つのエラーが両方出る")
        void create_deletedCategoryAndEmptyTitle_showsBothErrors() {
            // 準備
            Long deletedId = createDeletedCategoryId("削除されたカテゴリ");
            long before = taskRepository.count();

            // 実行・確認: タイトルのエラーがあっても、カテゴリを確かめずに画面を出し直さないことを確かめる
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "")
                    .param("priority", "MEDIUM")
                    .param("categoryId", deletedId.toString()))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText()
                    .contains("必須入力です")
                    .contains("選択したカテゴリは見つかりません。選び直してください");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("カテゴリが数値でない値だとエラーになり、保存されない")
        void create_nonNumericCategory_showsErrorAndDoesNotSave() {
            // 準備
            long before = taskRepository.count();

            // 実行・確認: 送信内容を書き換えた場合を想定し、IDとして読めない値を送る
            assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                    .param("title", "カテゴリが不正なタスク")
                    .param("priority", "MEDIUM")
                    .param("categoryId", "abc"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("入力値が不正です");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.count()).isEqualTo(before);
        }
    }

    @Nested
    @DisplayName("編集の保存（POST /tasks/{id}）")
    class Update {
        @Test
        @DisplayName("存在しないタスクを保存しようとすると、一覧へ戻され、タスクが見つからないことを知らせる文が渡される")
        void update_notFound_redirectsToListWithAlertMessage() {
            // 準備: 編集画面を開いている間に、別の操作でタスクが削除された場合と同じ状態にするため、登録してから削除したIDを使う
            Task task = createTask("削除されるタスク");
            entityManager.remove(task);
            entityManager.flush();
            long before = taskRepository.count();

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                    .param("title", "保存しようとしたタイトル")
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks")
                    .flash().containsEntry("alertMessage", "タスクが見つかりません。");

            // 確認: 新しいタスクとして登録されていない
            assertThat(taskRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("タイトルを変更して保存すると、一覧へ遷移し更新される")
        void update_validTitle_updatesAndRedirects() {
            // 準備
            Task task = createTask("変更前のタイトル");
            String newTitle = "変更後のテストのタスク";

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                    .param("title", newTitle)
                    .param("priority", "MEDIUM"))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            assertThat(findTitle(task.getId())).isEqualTo(newTitle);
        }

        @Test
        @DisplayName("編集画面でタイトルを空にして保存すると、エラーになり更新されない")
        void update_emptyTitle_showsErrorAndDoesNotUpdate() {
            // 準備
            String originalTitle = "変更前のタイトル";
            Task task = createTask(originalTitle);

            // 実行・確認: エラーで戻った画面でも優先度を選び直せるよう、選択肢が表示されていることも確かめる
            assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                    .param("title", "")
                    .param("priority", "MEDIUM"))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("必須入力です")
                    .contains(">高</option>", ">中</option>", ">低</option>");

            // 確認
            clearPersistenceContext();
            assertThat(findTitle(task.getId())).isEqualTo(originalTitle);
        }

        @Test
        @DisplayName("編集で、カテゴリのあるタスクを「カテゴリなし」に変えると、カテゴリなしに更新される")
        void update_toNoCategory_removesCategory() {
            // 準備
            Category category = createCategory("外すカテゴリ");
            String title = "カテゴリを外すタスク";
            Task task = createTask(title, category);

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                    .param("title", title)
                    .param("priority", "MEDIUM")
                    .param("categoryId", ""))
                    .hasStatus(HttpStatus.FOUND)
                    .hasRedirectedUrl(contextPath + "/tasks");

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.findById(task.getId()).orElseThrow().getCategory()).isNull();
        }

        @Test
        @DisplayName("存在しないカテゴリで更新すると、文言が出て更新されない")
        void update_deletedCategory_showsErrorAndDoesNotUpdate() {
            // 準備
            Category category = createCategory("変更前のカテゴリ");
            String originalTitle = "変更前のタイトル";
            Task task = createTask(originalTitle, category);
            Long deletedId = createDeletedCategoryId("削除されたカテゴリ");

            // 実行・確認
            assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                    .param("title", "変更後のタイトル")
                    .param("priority", "MEDIUM")
                    .param("categoryId", deletedId.toString()))
                    .hasStatusOk()
                    .hasViewName("tasks/form")
                    .bodyText().contains("選択したカテゴリは見つかりません。選び直してください");

            // 確認
            clearPersistenceContext();
            Task notUpdated = taskRepository.findById(task.getId()).orElseThrow();
            assertThat(notUpdated.getTitle()).isEqualTo(originalTitle);
            assertThat(notUpdated.getCategory().getId()).isEqualTo(category.getId());
        }

        /**
         * データベースに保存されているタイトルを取得する。
         *
         * <p>
         * 永続性コンテキストにタスクがあるとデータベースの値ではなくそちらが返るため、
         * 呼び出す前に{@link #clearPersistenceContext()}で永続性コンテキストを空にしておく。
         *
         * @param id 対象のタスクのID
         * @return タイトル
         */
        private String findTitle(Long id) {
            return taskRepository.findById(id).orElseThrow().getTitle();
        }
    }

    /**
     * テストの準備用に、タイトルだけを指定したタスクを登録する。
     *
     * <p>
     * 保存の方法は{@link #createTask(String, TaskStatus)}と同じ。
     *
     * @param title 登録するタイトル
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title) {
        return createTask(title, TaskStatus.TODO);
    }

    /**
     * テストの準備用に、タイトルとステータスを指定したタスクを登録する。
     *
     * <p>
     * 検証対象のメソッドを準備に使わないよう、直接保存する。
     * 以降の処理がデータベースに実際に保存された値を使うよう、保存後にデータベースから読み直す。
     *
     * @param title  登録するタイトル
     * @param status 登録するステータス
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title, TaskStatus status) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(status);
        task.setSortOrder(SORT_ORDER_NOT_CHECKED);
        return entityManager.persistFlushFind(task);
    }

    /**
     * テストの準備用に、タイトル・期限・ステータスを指定したタスクを登録する。
     *
     * <p>
     * 保存の方法は{@link #createTask(String, TaskStatus)}と同じ。
     *
     * @param title   登録するタイトル
     * @param dueDate 登録する期限
     * @param status  登録するステータス
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title, LocalDate dueDate, TaskStatus status) {
        Task task = new Task();
        task.setTitle(title);
        task.setDueDate(dueDate);
        task.setStatus(status);
        task.setSortOrder(SORT_ORDER_NOT_CHECKED);
        return entityManager.persistFlushFind(task);
    }

    /**
     * テストの準備用に、タイトルとカテゴリを指定したタスクを登録する。
     *
     * <p>
     * 保存の方法は{@link #createTask(String, TaskStatus)}と同じ。
     *
     * @param title    登録するタイトル
     * @param category 登録するカテゴリ
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title, Category category) {
        Task task = new Task();
        task.setTitle(title);
        task.setCategory(category);
        task.setSortOrder(SORT_ORDER_NOT_CHECKED);
        return entityManager.persistFlushFind(task);
    }

    /**
     * テストの準備用に、名前だけを指定したカテゴリを登録する。
     *
     * <p>
     * 検証対象のメソッドを準備に使わないよう、直接保存する。
     *
     * @param name 登録するカテゴリ名
     * @return データベースから読み直したカテゴリ
     */
    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return entityManager.persistFlushFind(category);
    }

    /**
     * テストの準備用に、名前とラベル色を指定したカテゴリを登録する。
     *
     * <p>
     * 保存の方法は{@link #createCategory(String)}と同じ。
     *
     * @param name  登録するカテゴリ名
     * @param color 登録するラベル色。#rrggbb形式
     * @return データベースから読み直したカテゴリ
     */
    private Category createCategory(String name, String color) {
        Category category = new Category();
        category.setName(name);
        category.setColor(color);
        return entityManager.persistFlushFind(category);
    }

    /**
     * テストの準備用に、カテゴリを登録してから削除し、存在しないカテゴリのIDを作る。
     *
     * <p>
     * 画面を開いている間に、別の操作でカテゴリが削除された場合と同じ状態にするため、登録してから削除したIDを使う。
     *
     * @param name 登録するカテゴリ名
     * @return 削除したカテゴリのID
     */
    private Long createDeletedCategoryId(String name) {
        Category category = createCategory(name);
        entityManager.remove(category);
        entityManager.flush();
        return category.getId();
    }

    /**
     * 追加・編集画面のHTMLから、カテゴリのプルダウンを取り出す。
     *
     * <p>
     * 優先度のプルダウンと区別して確かめるために使う。
     *
     * @param html 追加・編集画面のHTML
     * @return カテゴリのプルダウンの開始タグから、終了タグの前までのHTML
     */
    private String extractCategorySelect(String html) {
        int start = html.indexOf("id=\"categoryId\"");
        assertThat(start).as("カテゴリのプルダウン").isNotNegative();
        int end = html.indexOf("</select>", start);
        return html.substring(start, end);
    }

    /**
     * 永続性コンテキストの変更をデータベースへ反映し、永続性コンテキストを空にする。
     *
     * <p>
     * 取得時に永続性コンテキストのタスクではなくデータベースに保存された値を読むため、確認の前に呼び出す。
     */
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
