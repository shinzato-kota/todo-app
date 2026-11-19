package com.shinzato.todoapp.service.task;

import com.shinzato.todoapp.dto.task.CategoryCondition;
import com.shinzato.todoapp.dto.task.TaskInput;
import com.shinzato.todoapp.dto.task.TaskSearchCondition;
import com.shinzato.todoapp.dto.task.TaskStatusChangeResult;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.DueDateCondition;
import com.shinzato.todoapp.enums.task.DueDateStatus;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.exception.task.TaskNotFoundException;
import com.shinzato.todoapp.exception.task.TaskOrderConflictException;
import com.shinzato.todoapp.repository.task.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TaskService}のテスト。
 *
 * <p>
 * 開発用のデータベースに接続して確認するため、テストでの変更が残らないよう@Transactionalを付けている。
 */
@SpringBootTest(classes = TaskServiceTest.FixedClockConfig.class)
@AutoConfigureTestEntityManager
@Transactional
class TaskServiceTest {

    /** 並び順を確かめないテストで使う表示順。すべて同じ値にして、一覧の並びをIDの順（登録した順）にする */
    private static final int SORT_ORDER_NOT_CHECKED = 1;

    /** 期限の判定のテストで「今日」とする日付 */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    /** どのタスクのIDでもない値。IDは1から振られるため、負の数は存在しない */
    private static final Long NON_EXISTENT_TASK_ID = -1L;

    /**
     * 「今日」を固定した{@link Clock}を、アプリの{@link Clock}より優先して使わせる設定。
     */
    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            ZoneId zone = ZoneId.of("Asia/Tokyo");
            return Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
        }
    }

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("一覧の取得（getTaskList）")
    class GetTaskList {
        @Test
        @DisplayName("一覧は表示順に並ぶ")
        void getTaskList_ordersBySortOrder() {
            // 準備: 登録した順と表示順が逆になるよう、先に登録するタスクを表示順20、後に登録するタスクを表示順10にする
            String laterInList = "先に登録し、表示順が大きいタスク";
            String earlierInList = "後に登録し、表示順が小さいタスク";
            createTask(laterInList, 20);
            createTask(earlierInList, 10);

            // 実行
            List<TaskSummary> tasks = taskService.getTaskList();

            // 確認: 登録した順ではなく、表示順の小さい順に現れる
            assertThat(tasks).extracting(TaskSummary::title)
                    .containsSubsequence(earlierInList, laterInList);
        }

        @Test
        @DisplayName("期限がないタスクは、期限の状況が、期限切れでも今日が期限でもない（NORMAL）になる")
        void getTaskList_noDueDate_returnsNormal() {
            // 準備
            Task created = createTask("期限のないタスク", null, TaskStatus.TODO);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.NORMAL);
        }

        @Test
        @DisplayName("期限が昨日の未着手のタスクは、期限切れ（OVERDUE）になる")
        void getTaskList_pastDueTodo_returnsOverdue() {
            // 準備
            Task created = createTask("期限が昨日の未着手のタスク", TODAY.minusDays(1), TaskStatus.TODO);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.OVERDUE);
        }

        @Test
        @DisplayName("期限が昨日の進行中のタスクは、期限切れ（OVERDUE）になる")
        void getTaskList_pastDueInProgress_returnsOverdue() {
            // 準備
            Task created = createTask("期限が昨日の進行中のタスク", TODAY.minusDays(1), TaskStatus.IN_PROGRESS);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.OVERDUE);
        }

        @Test
        @DisplayName("期限が昨日の保留のタスクは、期限切れ（OVERDUE）になる")
        void getTaskList_pastDueOnHold_returnsOverdue() {
            // 準備
            Task created = createTask("期限が昨日の保留のタスク", TODAY.minusDays(1), TaskStatus.ON_HOLD);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.OVERDUE);
        }

        @Test
        @DisplayName("期限が昨日の完了のタスクは、期限切れ（OVERDUE）にならず、期限切れでも今日が期限でもない（NORMAL）になる")
        void getTaskList_pastDueDone_returnsNormal() {
            // 準備
            Task created = createTask("期限が昨日の完了のタスク", TODAY.minusDays(1), TaskStatus.DONE);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.NORMAL);
        }

        @Test
        @DisplayName("期限が昨日の中止のタスクは、期限切れ（OVERDUE）にならず、期限切れでも今日が期限でもない（NORMAL）になる")
        void getTaskList_pastDueCancelled_returnsNormal() {
            // 準備
            Task created = createTask("期限が昨日の中止のタスク", TODAY.minusDays(1), TaskStatus.CANCELLED);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.NORMAL);
        }

        @Test
        @DisplayName("期限が今日の未着手のタスクは、今日が期限（DUE_TODAY）になる")
        void getTaskList_dueTodayTodo_returnsDueToday() {
            // 準備
            Task created = createTask("期限が今日の未着手のタスク", TODAY, TaskStatus.TODO);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.DUE_TODAY);
        }

        @Test
        @DisplayName("期限が今日の進行中のタスクは、今日が期限（DUE_TODAY）になる")
        void getTaskList_dueTodayInProgress_returnsDueToday() {
            // 準備
            Task created = createTask("期限が今日の進行中のタスク", TODAY, TaskStatus.IN_PROGRESS);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.DUE_TODAY);
        }

        @Test
        @DisplayName("期限が今日の保留のタスクは、今日が期限（DUE_TODAY）になる")
        void getTaskList_dueTodayOnHold_returnsDueToday() {
            // 準備
            Task created = createTask("期限が今日の保留のタスク", TODAY, TaskStatus.ON_HOLD);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.DUE_TODAY);
        }

        @Test
        @DisplayName("期限が今日の完了のタスクも、今日が期限（DUE_TODAY）になる")
        void getTaskList_dueTodayDone_returnsDueToday() {
            // 準備
            Task created = createTask("期限が今日の完了のタスク", TODAY, TaskStatus.DONE);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.DUE_TODAY);
        }

        @Test
        @DisplayName("期限が今日の中止のタスクも、今日が期限（DUE_TODAY）になる")
        void getTaskList_dueTodayCancelled_returnsDueToday() {
            // 準備
            Task created = createTask("期限が今日の中止のタスク", TODAY, TaskStatus.CANCELLED);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.DUE_TODAY);
        }

        @Test
        @DisplayName("期限が明日の未着手のタスクは、期限切れでも今日が期限でもない（NORMAL）になる")
        void getTaskList_dueTomorrowTodo_returnsNormal() {
            // 準備
            Task created = createTask("期限が明日の未着手のタスク", TODAY.plusDays(1), TaskStatus.TODO);

            // 実行・確認
            assertThat(findDueDateStatus(created.getId())).isEqualTo(DueDateStatus.NORMAL);
        }

        /**
         * 一覧を取得し、指定したタスクの期限の状況を返す。
         *
         * @param id 対象のタスクのID
         * @return 期限の状況
         */
        private DueDateStatus findDueDateStatus(Long id) {
            return taskService.getTaskList().stream()
                    .filter(task -> task.id().equals(id))
                    .findFirst()
                    .orElseThrow()
                    .dueDateStatus();
        }
    }

    @Nested
    @DisplayName("条件に合うタスクの取得（searchTasks）")
    class SearchTasks {
        @ParameterizedTest
        @EnumSource(TaskStatus.class)
        @DisplayName("ステータスで絞り込むと、そのステータスのタスクだけが返る")
        void searchTasks_byStatus_returnsOnlyThatStatus(TaskStatus status) {
            // 準備: ステータスごとに1件ずつ、ステータスの名前をタイトルにして登録する
            deleteAllTasks();
            for (TaskStatus each : TaskStatus.values()) {
                saveTask(each.name(), task -> task.setStatus(each));
            }

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(status, null, null, null, null)))
                    .containsExactly(status.name());
        }

        @ParameterizedTest
        @EnumSource(TaskPriority.class)
        @DisplayName("優先度で絞り込むと、その優先度のタスクだけが返る")
        void searchTasks_byPriority_returnsOnlyThatPriority(TaskPriority priority) {
            // 準備: 優先度ごとに1件ずつ、優先度の名前をタイトルにして登録する
            deleteAllTasks();
            for (TaskPriority each : TaskPriority.values()) {
                saveTask(each.name(), task -> task.setPriority(each));
            }

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, priority, null, null)))
                    .containsExactly(priority.name());
        }

        @Test
        @DisplayName("「今日まで」で絞り込むと、期限が今日の、完了・中止以外のタスクだけが返る")
        void searchTasks_dueToday_returnsOpenTasksDueToday() {
            // 準備
            deleteAllTasks();
            saveDueDateVariations();

            // 実行・確認
            assertThat(
                    titlesOfSearchResult(new TaskSearchCondition(null, DueDateCondition.DUE_TODAY, null, null, null)))
                    .containsExactly("期限が今日の未着手");
        }

        @Test
        @DisplayName("「期限切れのみ」で絞り込むと、期限が今日より前の、完了・中止以外のタスクだけが返る")
        void searchTasks_overdue_returnsOpenTasksPastDue() {
            // 準備
            deleteAllTasks();
            saveDueDateVariations();

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, DueDateCondition.OVERDUE, null, null, null)))
                    .containsExactly("期限が昨日の未着手");
        }

        @Test
        @DisplayName("キーワードで絞り込むと、タイトルか詳細にキーワードが含まれるタスクが返る")
        void searchTasks_byKeyword_matchesTitleOrDescription() {
            // 準備
            deleteAllTasks();
            saveTask("会議の資料を作る");
            saveTask("タイトルには含まれない", task -> task.setDescription("会議の前に読む"));
            saveTask("どこにも含まれない", task -> task.setDescription("詳細"));

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, "会議", null)))
                    .containsExactlyInAnyOrder("会議の資料を作る", "タイトルには含まれない");
        }

        @Test
        @DisplayName("キーワードの大文字と小文字は区別しない")
        void searchTasks_byKeyword_ignoresCase() {
            // 準備
            deleteAllTasks();
            saveTask("Report");

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, "rep", null)))
                    .containsExactly("Report");
        }

        @Test
        @DisplayName("キーワードの「%」は、何文字でもよいという意味ではなく、ただの文字として探す")
        void searchTasks_keywordWithPercent_matchesLiterally() {
            // 準備: 「%」が何文字でもよいという意味のままだと、「1000達成」も見つかる
            deleteAllTasks();
            saveTask("0%達成");
            saveTask("1000達成");

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, "0%", null)))
                    .containsExactly("0%達成");
        }

        @Test
        @DisplayName("キーワードの「_」は、1文字なら何でもよいという意味ではなく、ただの文字として探す")
        void searchTasks_keywordWithUnderscore_matchesLiterally() {
            // 準備: 「_」が1文字なら何でもよいという意味のままだと、「axb」も見つかる
            deleteAllTasks();
            saveTask("a_b");
            saveTask("axb");

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, "a_b", null)))
                    .containsExactly("a_b");
        }

        @Test
        @DisplayName("キーワードの「\\」は、エスケープ文字ではなく、ただの文字として探す")
        void searchTasks_keywordWithBackslash_matchesLiterally() {
            // 準備: 「\」がエスケープ文字のままだと、パターンの後ろに付けた「%」がただの文字になり、「%」で終わる「100%」が見つかる
            deleteAllTasks();
            saveTask("C:\\temp");
            saveTask("100%");

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, "\\", null)))
                    .containsExactly("C:\\temp");
        }

        @Test
        @DisplayName("カテゴリのIDで絞り込むと、そのカテゴリのタスクだけが返る")
        void searchTasks_byCategoryId_returnsOnlyThatCategory() {
            // 準備
            deleteAllTasks();
            Category target = createCategory("絞り込むカテゴリ");
            Category other = createCategory("絞り込まないカテゴリ");
            saveTask("絞り込むカテゴリのタスク", task -> task.setCategory(target));
            saveTask("ほかのカテゴリのタスク", task -> task.setCategory(other));
            saveTask("カテゴリなしのタスク");

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, null,
                    new CategoryCondition.ById(target.getId()))))
                    .containsExactly("絞り込むカテゴリのタスク");
        }

        @Test
        @DisplayName("カテゴリなしで絞り込むと、カテゴリのないタスクだけが返る")
        void searchTasks_noCategory_returnsOnlyTasksWithoutCategory() {
            // 準備
            deleteAllTasks();
            Category category = createCategory("絞り込みで使わないカテゴリ");
            saveTask("カテゴリのあるタスク", task -> task.setCategory(category));
            saveTask("カテゴリなしのタスク");

            // 実行・確認
            assertThat(
                    titlesOfSearchResult(new TaskSearchCondition(null, null, null, null, new CategoryCondition.None())))
                    .containsExactly("カテゴリなしのタスク");
        }

        @Test
        @DisplayName("存在しないカテゴリのIDで絞り込むと、0件になる")
        void searchTasks_missingCategoryId_returnsEmpty() {
            // 準備
            deleteAllTasks();
            Long missingCategoryId = createDeletedCategoryId("削除するカテゴリ");
            saveTask("カテゴリなしのタスク");

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, null,
                    new CategoryCondition.ById(missingCategoryId))))
                    .isEmpty();
        }

        @Test
        @DisplayName("ステータスと優先度で絞り込むと、両方に合うタスクだけが返る")
        void searchTasks_byStatusAndPriority_returnsTasksMatchingBoth() {
            // 準備
            deleteAllTasks();
            saveTask("進行中・高", task -> {
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setPriority(TaskPriority.HIGH);
            });
            saveTask("進行中・低", task -> {
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setPriority(TaskPriority.LOW);
            });
            saveTask("未着手・高", task -> {
                task.setStatus(TaskStatus.TODO);
                task.setPriority(TaskPriority.HIGH);
            });

            // 実行・確認
            assertThat(
                    titlesOfSearchResult(new TaskSearchCondition(TaskStatus.IN_PROGRESS, null, TaskPriority.HIGH, null,
                            null)))
                    .containsExactly("進行中・高");
        }

        @Test
        @DisplayName("5つの条件で絞り込むと、すべての条件に合うタスクだけが返る")
        void searchTasks_allConditions_returnsTasksMatchingAll() {
            // 準備: すべてに合うタスクと、条件を1つずつ外したタスクを登録する
            deleteAllTasks();
            Category target = createCategory("5つの条件で使うカテゴリ");
            Category other = createCategory("5つの条件で使わないカテゴリ");
            saveTask("すべてに合う会議", task -> setAllConditions(task, target));
            saveTask("ステータスだけ違う会議", task -> {
                setAllConditions(task, target);
                task.setStatus(TaskStatus.TODO);
            });
            saveTask("期限だけ違う会議", task -> {
                setAllConditions(task, target);
                task.setDueDate(TODAY.plusDays(1));
            });
            saveTask("優先度だけ違う会議", task -> {
                setAllConditions(task, target);
                task.setPriority(TaskPriority.LOW);
            });
            saveTask("キーワードに合わない打ち合わせ", task -> setAllConditions(task, target));
            saveTask("カテゴリだけ違う会議", task -> setAllConditions(task, other));
            TaskSearchCondition condition = new TaskSearchCondition(TaskStatus.IN_PROGRESS, DueDateCondition.DUE_TODAY,
                    TaskPriority.HIGH, "会議", new CategoryCondition.ById(target.getId()));

            // 実行・確認
            assertThat(titlesOfSearchResult(condition)).containsExactly("すべてに合う会議");
        }

        @Test
        @DisplayName("条件がない場合は、全件が表示順の昇順、同じ場合はIDの昇順で返る")
        void searchTasks_noConditions_returnsAllInListOrder() {
            // 準備: 表示順30・20・10・10の順に登録し、登録した順と表示順が逆になるようにし、表示順が同じタスクも2件登録する
            deleteAllTasks();
            saveTask("表示順30", task -> task.setSortOrder(30));
            saveTask("表示順20", task -> task.setSortOrder(20));
            saveTask("表示順10・先に登録", task -> task.setSortOrder(10));
            saveTask("表示順10・後に登録", task -> task.setSortOrder(10));

            // 実行・確認
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, null, null)))
                    .containsExactly("表示順10・先に登録", "表示順10・後に登録", "表示順20", "表示順30");
        }

        /**
         * 期限の絞り込みのテストの準備用に、期限（なし・昨日・今日・明日）とステータス（未着手・完了・中止）を組み合わせたタスクを登録する。
         */
        private void saveDueDateVariations() {
            saveTask("期限なしの未着手", task -> task.setStatus(TaskStatus.TODO));
            saveTask("期限が昨日の未着手", task -> setDueDateAndStatus(task, TODAY.minusDays(1), TaskStatus.TODO));
            saveTask("期限が昨日の完了", task -> setDueDateAndStatus(task, TODAY.minusDays(1), TaskStatus.DONE));
            saveTask("期限が昨日の中止", task -> setDueDateAndStatus(task, TODAY.minusDays(1), TaskStatus.CANCELLED));
            saveTask("期限が今日の未着手", task -> setDueDateAndStatus(task, TODAY, TaskStatus.TODO));
            saveTask("期限が今日の完了", task -> setDueDateAndStatus(task, TODAY, TaskStatus.DONE));
            saveTask("期限が今日の中止", task -> setDueDateAndStatus(task, TODAY, TaskStatus.CANCELLED));
            saveTask("期限が明日の未着手", task -> setDueDateAndStatus(task, TODAY.plusDays(1), TaskStatus.TODO));
        }

        /**
         * タスクに期限とステータスを設定する。
         *
         * @param task    設定するタスク
         * @param dueDate 期限
         * @param status  ステータス
         */
        private void setDueDateAndStatus(Task task, LocalDate dueDate, TaskStatus status) {
            task.setDueDate(dueDate);
            task.setStatus(status);
        }

        /**
         * 5つの条件のテストの準備用に、タスクを条件（進行中・今日が期限・高・カテゴリ）に合わせる。
         *
         * <p>
         * キーワードの条件は、タイトルで合わせる。
         *
         * @param task     設定するタスク
         * @param category 設定するカテゴリ
         */
        private void setAllConditions(Task task, Category category) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setDueDate(TODAY);
            task.setPriority(TaskPriority.HIGH);
            task.setCategory(category);
        }

        /**
         * 絞り込みのテストの準備用に、タイトルだけを指定したタスクを登録する。
         *
         * <p>
         * 保存の方法は{@link #saveTask(String, Consumer)}と同じ。
         *
         * @param title 登録するタイトル
         */
        private void saveTask(String title) {
            saveTask(title, task -> {
            });
        }

        /**
         * 絞り込みのテストの準備用に、タスクを登録する。
         *
         * <p>
         * 表示順は{@link #SORT_ORDER_NOT_CHECKED}にし、ほかの項目は{@code settings}で設定する。
         * 保存の方法は{@link #createTask(String)}と同じ。
         *
         * @param title    登録するタイトル
         * @param settings タイトルと表示順のほかに設定する項目
         */
        private void saveTask(String title, Consumer<Task> settings) {
            Task task = new Task();
            task.setTitle(title);
            task.setSortOrder(SORT_ORDER_NOT_CHECKED);
            settings.accept(task);
            entityManager.persistFlushFind(task);
        }

        /**
         * テストの準備用に、名前を指定したカテゴリを登録する。
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
         * テストの準備用に、カテゴリを登録してから削除し、存在しないカテゴリのIDを作る。
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
    }

    @Nested
    @DisplayName("新規登録（registerTask）")
    class RegisterTask {
        @Test
        @DisplayName("タスクを登録すると、ステータスが未着手になる")
        void registerTask_setsStatusToTodo() {
            // 準備: 開発用のデータが残っていても判定できるよう、登録前の件数を控える
            String title = "買い物に行く";
            TaskInput input = new TaskInput(title, null, null, TaskPriority.MEDIUM, null);
            long before = taskRepository.count();

            // 実行
            taskService.registerTask(input);

            // 確認
            clearPersistenceContext();
            List<TaskSummary> tasks = taskService.getTaskList();
            assertThat(tasks).hasSize((int) before + 1);
            TaskSummary registered = tasks.get(tasks.size() - 1);
            assertThat(registered.title()).isEqualTo(title);
            assertThat(registered.status()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("新しく登録したタスクは、一覧で今あるどのタスクよりも後に並ぶ")
        void registerTask_placesAfterExistingTasks() {
            // 準備: 既存のどのタスクよりも表示順が大きいタスクを、表示順1000で登録しておく
            createTask("登録前に最後にあるタスク", 1000);
            TaskInput input = new TaskInput("新しく登録するタスク", null, null, TaskPriority.MEDIUM, null);

            // 実行
            taskService.registerTask(input);

            // 確認: 新しく登録したタスクが、それまで最後にあったタスクの後ろに並ぶ
            clearPersistenceContext();
            List<TaskSummary> tasks = taskService.getTaskList();
            assertThat(tasks).extracting(TaskSummary::title)
                    .endsWith("登録前に最後にあるタスク", "新しく登録するタスク");
        }

        @Test
        @DisplayName("タスクを登録すると、詳細・期限・優先度も保存される")
        void registerTask_savesDescriptionDueDateAndPriority() {
            // 準備: 優先度は初期値(中)と区別できるよう、高にする
            TaskInput input = new TaskInput("詳細・期限・優先度つきのタスク", "1行目\n2行目", LocalDate.of(2026, 10, 31),
                    TaskPriority.HIGH, null);

            // 実行
            taskService.registerTask(input);

            // 確認: 新しく登録したタスクは表示順が最も大きくなるため、その1件を読み直す
            clearPersistenceContext();
            Task registered = taskRepository.findFirstByOrderBySortOrderDesc().orElseThrow();
            assertThat(registered.getTitle()).isEqualTo("詳細・期限・優先度つきのタスク");
            assertThat(registered.getDescription()).isEqualTo("1行目\n2行目");
            assertThat(registered.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 31));
            assertThat(registered.getPriority()).isEqualTo(TaskPriority.HIGH);
        }
    }

    @Nested
    @DisplayName("更新（updateTask）")
    class UpdateTask {
        @Test
        @DisplayName("タスクのタイトルを更新できる")
        void updateTask_changesTitle() {
            // 準備
            String updatedTitle = "変更後のタイトル";
            Task created = createTask("変更前のタイトル");
            TaskInput input = new TaskInput(updatedTitle, null, null, TaskPriority.MEDIUM, null);

            // 実行
            taskService.updateTask(created.getId(), input);

            // 確認
            clearPersistenceContext();
            assertThat(taskRepository.findById(created.getId()).orElseThrow().getTitle())
                    .isEqualTo(updatedTitle);
        }

        @Test
        @DisplayName("タスクを更新すると、詳細・期限・優先度も変わる")
        void updateTask_changesDescriptionDueDateAndPriority() {
            // 準備: 詳細・期限なし、優先度が中のタスクを、すべて値のある状態へ変える
            Task created = createTask("更新するタスク");
            TaskInput input = new TaskInput("更新するタスク", "追加した詳細", LocalDate.of(2026, 10, 31), TaskPriority.LOW,
                    null);

            // 実行
            taskService.updateTask(created.getId(), input);

            // 確認
            clearPersistenceContext();
            Task updated = taskRepository.findById(created.getId()).orElseThrow();
            assertThat(updated.getDescription()).isEqualTo("追加した詳細");
            assertThat(updated.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 31));
            assertThat(updated.getPriority()).isEqualTo(TaskPriority.LOW);
        }
    }

    @Nested
    @DisplayName("ステータスの変更（changeStatus）")
    class ChangeStatus {
        @Test
        @DisplayName("ステータスを完了に変更すると、完了日時が設定される")
        void changeStatus_toDone_setsCompletedAt() {
            // 準備
            Task created = createTask("完了にするタスク");

            // 実行
            TaskStatusChangeResult updated = taskService.changeStatus(created.getId(), TaskStatus.DONE);

            // 確認: 戻り値だけでなく、データベースに保存された値も確認する
            assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
            clearPersistenceContext();
            assertThat(findStatus(created.getId())).isEqualTo(TaskStatus.DONE);
            assertThat(findCompletedAt(created.getId())).isNotNull();
        }

        @Test
        @DisplayName("完了から他のステータスへ変更すると、完了日時が解除される")
        void changeStatus_fromDone_clearsCompletedAt() {
            // 準備: 一度完了にして完了日時を持たせる
            Task created = createTask("完了から戻すタスク");
            taskService.changeStatus(created.getId(), TaskStatus.DONE);

            // 実行
            TaskStatusChangeResult updated = taskService.changeStatus(created.getId(), TaskStatus.IN_PROGRESS);

            // 確認: 戻り値だけでなく、データベースに保存された値も確認する
            assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            clearPersistenceContext();
            assertThat(findStatus(created.getId())).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(findCompletedAt(created.getId())).isNull();
        }

        @Test
        @DisplayName("すでに完了のタスクを完了に変更しても、完了日時は変わらない")
        void changeStatus_doneToDone_keepsCompletedAt() {
            // 準備: 完了日時を、テストを実行する日と違う2026-01-05 09:30にして、完了のタスクを登録する
            Task task = new Task();
            task.setTitle("完了のまま完了にするタスク");
            task.setSortOrder(SORT_ORDER_NOT_CHECKED);
            task.setStatus(TaskStatus.DONE);
            task.setCompletedAt(LocalDateTime.of(2026, 1, 5, 9, 30));
            Task created = entityManager.persistFlushFind(task);

            // 実行
            taskService.changeStatus(created.getId(), TaskStatus.DONE);

            // 確認
            clearPersistenceContext();
            assertThat(findCompletedAt(created.getId())).isEqualTo(LocalDateTime.of(2026, 1, 5, 9, 30));
        }

        @Test
        @DisplayName("保留へ変更でき、完了日時は設定されない")
        void changeStatus_toOnHold_doesNotSetCompletedAt() {
            // 準備
            Task created = createTask("保留にするタスク");

            // 実行
            TaskStatusChangeResult updated = taskService.changeStatus(created.getId(), TaskStatus.ON_HOLD);

            // 確認: 戻り値だけでなく、データベースに保存された値も確認する
            assertThat(updated.status()).isEqualTo(TaskStatus.ON_HOLD);
            clearPersistenceContext();
            assertThat(findStatus(created.getId())).isEqualTo(TaskStatus.ON_HOLD);
            assertThat(findCompletedAt(created.getId())).isNull();
        }

        @Test
        @DisplayName("中止へ変更でき、完了日時は設定されない")
        void changeStatus_toCancelled_doesNotSetCompletedAt() {
            // 準備
            Task created = createTask("中止にするタスク");

            // 実行
            TaskStatusChangeResult updated = taskService.changeStatus(created.getId(), TaskStatus.CANCELLED);

            // 確認: 戻り値だけでなく、データベースに保存された値も確認する
            assertThat(updated.status()).isEqualTo(TaskStatus.CANCELLED);
            clearPersistenceContext();
            assertThat(findStatus(created.getId())).isEqualTo(TaskStatus.CANCELLED);
            assertThat(findCompletedAt(created.getId())).isNull();
        }

        @Test
        @DisplayName("変更後のステータスがnullの場合、例外が発生する")
        void changeStatus_nullStatus_throwsNullPointerException() {
            // 準備: 存在するタスクを使い、IDではなくステータスのチェックで止まることを確かめる
            Task created = createTask("ステータスをnullにするタスク");

            // 実行・確認
            assertThatThrownBy(() -> taskService.changeStatus(created.getId(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("newStatusはnullであってはいけません");
        }

        @Test
        @DisplayName("期限切れのタスクを完了にすると、結果の期限の状況が、期限切れでも今日が期限でもない（NORMAL）になる")
        void changeStatus_overdueToDone_returnsNormal() {
            // 準備
            Task created = createTask("期限切れを完了にするタスク", TODAY.minusDays(1), TaskStatus.TODO);

            // 実行
            TaskStatusChangeResult updated = taskService.changeStatus(created.getId(), TaskStatus.DONE);

            // 確認
            assertThat(updated.dueDateStatus()).isEqualTo(DueDateStatus.NORMAL);
        }

        @Test
        @DisplayName("期限が過ぎた完了のタスクを未着手に戻すと、結果の期限の状況が期限切れ（OVERDUE）になる")
        void changeStatus_pastDueDoneToTodo_returnsOverdue() {
            // 準備
            Task created = createTask("期限が過ぎた完了のタスク", TODAY.minusDays(1), TaskStatus.DONE);

            // 実行
            TaskStatusChangeResult updated = taskService.changeStatus(created.getId(), TaskStatus.TODO);

            // 確認
            assertThat(updated.dueDateStatus()).isEqualTo(DueDateStatus.OVERDUE);
        }

        /**
         * データベースに保存されているステータスを取得する。
         *
         * <p>
         * 永続性コンテキストにタスクがあるとデータベースの値ではなくそちらが返るため、
         * 呼び出す前に{@link #clearPersistenceContext()}で永続性コンテキストを空にしておく。
         *
         * @param id 対象のタスクのID
         * @return ステータス
         */
        private TaskStatus findStatus(Long id) {
            return taskRepository.findById(id).orElseThrow().getStatus();
        }

        /**
         * データベースに保存されている完了日時を取得する。
         *
         * <p>
         * 永続性コンテキストにタスクがあるとデータベースの値ではなくそちらが返るため、
         * 呼び出す前に{@link #clearPersistenceContext()}で永続性コンテキストを空にしておく。
         *
         * @param id 対象のタスクのID
         * @return 完了日時。設定されていない場合はnull
         */
        private LocalDateTime findCompletedAt(Long id) {
            return taskRepository.findById(id).orElseThrow().getCompletedAt();
        }
    }

    @Nested
    @DisplayName("表示順の変更（reorderTasks）")
    class ReorderTasks {
        @Test
        @DisplayName("データベースにあるすべてのタスクのIDを送ると、送った順に1から表示順が保存される")
        void reorderTasks_allIds_savesSortOrderFromOneInSentOrder() {
            // 準備: 3件を表示順10・20・30で登録する。実行で送る並び（3件目、1件目、2件目）は、登録の順と違う
            // 登録の順に振る誤りや、今の表示順の値を使い回して1から振り直さない誤りでも、確認の値（1・2・3）と食い違うようにするため
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);
            Task third = createTask("並び替えるタスク3", 30);

            // 実行
            taskService.reorderTasks(List.of(third.getId(), first.getId(), second.getId()));

            // 確認
            clearPersistenceContext();
            assertThat(findSortOrder(third.getId())).isEqualTo(1);
            assertThat(findSortOrder(first.getId())).isEqualTo(2);
            assertThat(findSortOrder(second.getId())).isEqualTo(3);
            assertThat(titlesOfSearchResult(new TaskSearchCondition(null, null, null, null, null)))
                    .containsExactly("並び替えるタスク3", "並び替えるタスク1", "並び替えるタスク2");
        }

        @Test
        @DisplayName("タスクが1件のときは、そのIDを送ると表示順が1になる")
        void reorderTasks_oneTask_savesSortOrderOne() {
            // 準備
            deleteAllTasks();
            Task task = createTask("1件だけのタスク", 5);

            // 実行
            taskService.reorderTasks(List.of(task.getId()));

            // 確認
            clearPersistenceContext();
            assertThat(findSortOrder(task.getId())).isEqualTo(1);
        }

        @Test
        @DisplayName("タスクが0件のときは、空の並びを送っても例外にならない")
        void reorderTasks_noTasksAndEmptyList_doesNotThrow() {
            // 準備
            deleteAllTasks();

            // 実行・確認
            assertThatCode(() -> taskService.reorderTasks(List.of())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("IDが足りない並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_missingId_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);

            // 実行・確認
            assertReorderConflict(List.of(second.getId()), first, second);
        }

        @Test
        @DisplayName("タスクがあるのに空の並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_emptyListWithTasks_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);

            // 実行・確認
            assertReorderConflict(List.of(), first, second);
        }

        @Test
        @DisplayName("データベースにあるすべてのタスクのIDに加えて、存在しないIDも入っている並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_extraId_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);

            // 実行・確認
            assertReorderConflict(List.of(second.getId(), first.getId(), NON_EXISTENT_TASK_ID), first, second);
        }

        @Test
        @DisplayName("IDの数は合うが、存在しないIDが入っている並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_nonExistentIdInsteadOfTask_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);

            // 実行・確認
            assertReorderConflict(List.of(second.getId(), NON_EXISTENT_TASK_ID), first, second);
        }

        @Test
        @DisplayName("IDの数は合うが、重複したIDが入っている並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_duplicateIdInsteadOfTask_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);
            Task third = createTask("並び替えるタスク3", 30);

            // 実行・確認
            assertReorderConflict(List.of(second.getId(), second.getId(), first.getId()), first, second, third);
        }

        @Test
        @DisplayName("データベースにあるすべてのタスクのIDを含むが、重複したIDも入っている並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_allIdsWithDuplicate_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);

            // 実行・確認
            assertReorderConflict(List.of(second.getId(), first.getId(), second.getId()), first, second);
        }

        @Test
        @DisplayName("nullが入っている並びを送ると例外になり、表示順は変わらない")
        void reorderTasks_nullInList_throwsAndKeepsSortOrder() {
            // 準備
            deleteAllTasks();
            Task first = createTask("並び替えるタスク1", 10);
            Task second = createTask("並び替えるタスク2", 20);

            // 実行・確認
            assertReorderConflict(Arrays.asList(second.getId(), null), first, second);
        }

        @Test
        @DisplayName("並び替えると、表示順が変わったタスクだけ更新日時が変わる")
        void reorderTasks_updatesUpdatedAtOnlyOfChangedTasks() {
            // 準備: 先頭のタスクは表示順が1のまま変わらないようにし、更新日時を過去の日時にそろえる
            deleteAllTasks();
            Task unchanged = createTask("表示順が変わらないタスク", 1);
            Task movedDown = createTask("表示順が大きくなるタスク", 2);
            Task movedUp = createTask("表示順が小さくなるタスク", 3);
            LocalDateTime past = LocalDateTime.of(2000, 1, 1, 0, 0);
            entityManager.getEntityManager().createNativeQuery("UPDATE task SET updated_at = :past")
                    .setParameter("past", past)
                    .executeUpdate();
            clearPersistenceContext();

            // 実行
            taskService.reorderTasks(List.of(unchanged.getId(), movedUp.getId(), movedDown.getId()));

            // 確認
            clearPersistenceContext();
            assertThat(findUpdatedAt(unchanged.getId())).isEqualTo(past);
            assertThat(findUpdatedAt(movedDown.getId())).isAfter(past);
            assertThat(findUpdatedAt(movedUp.getId())).isAfter(past);
        }

        /**
         * 並びを送ると例外になり、渡したタスクの表示順が変わらないことを確かめる。
         *
         * @param taskIds 送るIDの並び
         * @param tasks   表示順が変わらないことを確かめるタスク
         */
        private void assertReorderConflict(List<Long> taskIds, Task... tasks) {
            // 渡されたタスクは永続性コンテキストにあり、サービスが書き換えると一緒に変わるため、呼び出す前に値を控える
            Map<Long, Integer> sortOrdersBefore = Arrays.stream(tasks)
                    .collect(Collectors.toMap(Task::getId, Task::getSortOrder));

            assertThatThrownBy(() -> taskService.reorderTasks(taskIds))
                    .isInstanceOf(TaskOrderConflictException.class);

            clearPersistenceContext();
            sortOrdersBefore.forEach((id, sortOrder) -> assertThat(findSortOrder(id)).isEqualTo(sortOrder));
        }

        /**
         * データベースに保存されている表示順を取得する。
         *
         * <p>
         * 永続性コンテキストにタスクがあるとデータベースの値ではなくそちらが返るため、
         * 呼び出す前に{@link #clearPersistenceContext()}で永続性コンテキストを空にしておく。
         *
         * @param id 対象のタスクのID
         * @return 表示順
         */
        private Integer findSortOrder(Long id) {
            return taskRepository.findById(id).orElseThrow().getSortOrder();
        }

        /**
         * データベースに保存されている更新日時を取得する。
         *
         * <p>
         * 永続性コンテキストにタスクがあるとデータベースの値ではなくそちらが返るため、
         * 呼び出す前に{@link #clearPersistenceContext()}で永続性コンテキストを空にしておく。
         *
         * @param id 対象のタスクのID
         * @return 更新日時
         */
        private LocalDateTime findUpdatedAt(Long id) {
            return taskRepository.findById(id).orElseThrow().getUpdatedAt();
        }
    }

    @Nested
    @DisplayName("削除（removeTask）")
    class RemoveTask {
        @Test
        @DisplayName("削除すると、指定したタスクだけがデータベースから取り除かれる")
        void removeTask_deletesFromDatabase() {
            // 準備: 削除するタスクと、消えないことを確かめるタスクを用意する
            long before = taskRepository.count();
            Task deletedTask = createTask("削除するタスク");
            Task remainingTask = createTask("削除の対象外のタスク");

            // 実行
            taskService.removeTask(deletedTask.getId());

            // 確認: 指定したタスクだけが消え、件数は1件だけ減る
            clearPersistenceContext();
            assertThat(taskRepository.findById(deletedTask.getId())).isEmpty();
            assertThat(taskRepository.findById(remainingTask.getId())).isPresent();
            assertThat(taskRepository.count()).isEqualTo(before + 1);
        }

        @Test
        @DisplayName("存在しないタスクを削除しようとすると例外が発生する")
        void removeTask_notFound_throwsException() {
            assertThatThrownBy(() -> taskService.removeTask(-1L))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    /**
     * テストの準備用に、登録済みのタスクをすべて削除する。
     *
     * <p>
     * 開発用のデータベースにあるタスクが、検索の結果や、並び替えで送るすべてのタスクのIDに混ざらないようにする。削除はテストの終了時にロールバックされる。
     */
    private void deleteAllTasks() {
        taskRepository.deleteAll();
    }

    /**
     * 条件で絞り込み、結果のタスクのタイトルを返す。
     *
     * @param condition 絞り込みの条件
     * @return 絞り込んだ結果のタイトル。結果の順に並ぶ
     */
    private List<String> titlesOfSearchResult(TaskSearchCondition condition) {
        return taskService.searchTasks(condition).stream()
                .map(TaskSummary::title)
                .toList();
    }

    /**
     * テストの準備用に、タイトルだけを指定したタスクを登録する。
     *
     * <p>
     * 検証対象のメソッドを準備に使わないよう、直接保存する。
     * 以降の処理がデータベースに実際に保存された値を使うよう、保存後にデータベースから読み直す。
     *
     * @param title 登録するタイトル
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title) {
        return createTask(title, SORT_ORDER_NOT_CHECKED);
    }

    /**
     * テストの準備用に、タイトルと表示順を指定したタスクを登録する。
     *
     * <p>
     * 保存の方法は{@link #createTask(String)}と同じ。
     *
     * @param title     登録するタイトル
     * @param sortOrder 登録する表示順
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title, int sortOrder) {
        Task task = new Task();
        task.setTitle(title);
        task.setSortOrder(sortOrder);
        return entityManager.persistFlushFind(task);
    }

    /**
     * テストの準備用に、期限とステータスを指定したタスクを登録する。
     *
     * <p>
     * 保存の方法は{@link #createTask(String)}と同じ。
     *
     * @param title   登録するタイトル
     * @param dueDate 登録する期限。期限を設定しない場合はnull
     * @param status  登録するステータス
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title, LocalDate dueDate, TaskStatus status) {
        Task task = new Task();
        task.setTitle(title);
        task.setSortOrder(SORT_ORDER_NOT_CHECKED);
        task.setDueDate(dueDate);
        task.setStatus(status);
        return entityManager.persistFlushFind(task);
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
