package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.repository.task.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskApiController}のテスト。
 *
 * <p>
 * jQueryから呼び出すAPIと同じ形式でリクエストを送り、レスポンスを確認する。
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
class TaskApiControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** 設定値と同じコンテキストパスを使うため、プロパティから読み込む */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Nested
    @DisplayName("検索（GET /api/tasks）")
    class Search {
        @Test
        @DisplayName("絞り込むと、200と、条件に合うタスクの配列が返る")
        void search_returnsArrayOfMatchingTasks() {
            // 準備: 開発用のデータベースにあるタスクが結果に混ざらないよう、全件削除する(終了時にロールバックされる)
            taskRepository.deleteAll();
            Task inProgress = createTask("APIで絞り込む進行中のタスク");
            inProgress.setStatus(TaskStatus.IN_PROGRESS);
            createTask("APIで絞り込まない未着手のタスク");
            clearPersistenceContext();

            // 実行・確認
            assertThat(mockMvc.get().uri(contextPath + "/api/tasks")
                    .contextPath(contextPath)
                    .param("status", "IN_PROGRESS"))
                    .hasStatusOk()
                    .bodyJson()
                    .isLenientlyEqualTo("[{\"title\":\"APIで絞り込む進行中のタスク\",\"status\":\"IN_PROGRESS\"}]");
        }

        @Test
        @DisplayName("キーワードが100文字なら、受け付ける")
        void search_keywordOf100Characters_returnsOk() {
            assertThat(mockMvc.get().uri(contextPath + "/api/tasks")
                    .contextPath(contextPath)
                    .param("keyword", "あ".repeat(100)))
                    .hasStatusOk();
        }

        @Test
        @DisplayName("キーワードが101文字なら、400と項目別のエラーが返る")
        void search_keywordOf101Characters_returnsBadRequest() {
            assertThat(mockMvc.get().uri(contextPath + "/api/tasks")
                    .contextPath(contextPath)
                    .param("keyword", "あ".repeat(101)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"入力値が不正です\",\"errors\":{\"keyword\":\"100文字以内で入力してください\"}}");
        }

        @ParameterizedTest
        @ValueSource(strings = { "status", "due", "priority" })
        @DisplayName("ステータス・期限・優先度に存在しない値を送ると、400と内部情報を含まないエラーが返る")
        void search_invalidEnumValue_returnsBadRequestWithoutInternalInfo(String name) {
            assertThat(mockMvc.get().uri(contextPath + "/api/tasks")
                    .contextPath(contextPath)
                    .param(name, "XXX"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"入力値が不正です\",\"errors\":{\"" + name + "\":\"入力値が不正です\"}}");
        }

        @Test
        @DisplayName("カテゴリが18桁の数なら、受け付ける")
        void search_categoryOf18Digits_returnsOk() {
            assertThat(mockMvc.get().uri(contextPath + "/api/tasks")
                    .contextPath(contextPath)
                    .param("category", "123456789012345678"))
                    .hasStatusOk();
        }

        @ParameterizedTest
        @ValueSource(strings = { "abc", "none", "", "1234567890123456789", "99999999999999999999" })
        @DisplayName("カテゴリに数字でもNONEでもない値か、19桁以上の数を送ると、400と内部情報を含まないエラーが返る")
        void search_malformedCategory_returnsBadRequestWithoutInternalInfo(String category) {
            assertThat(mockMvc.get().uri(contextPath + "/api/tasks")
                    .contextPath(contextPath)
                    .param("category", category))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"入力値が不正です\",\"errors\":{\"category\":\"入力値が不正です\"}}");
        }
    }

    @Nested
    @DisplayName("並び替え（PUT /api/tasks/reorder）")
    class Reorder {
        @Test
        @DisplayName("データベースにあるすべてのタスクのIDを送ると、204が返り、送った順に表示順が保存される")
        void reorder_allIds_returnsNoContentAndSavesSortOrder() {
            // 準備: 開発用のデータベースにあるタスクがIDの比較に混ざらないよう、全件削除する(終了時にロールバックされる)
            taskRepository.deleteAll();
            Task first = createTask("APIで並び替えるタスク1");
            Task second = createTask("APIで並び替えるタスク2");

            // 実行・確認
            assertThat(mockMvc.put().uri(contextPath + "/api/tasks/reorder")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taskIds\":[" + second.getId() + "," + first.getId() + "]}"))
                    .hasStatus(HttpStatus.NO_CONTENT);

            // 確認: レスポンスだけでなく、データベースに保存された値も確認する
            clearPersistenceContext();
            assertThat(findSortOrder(second.getId())).isEqualTo(1);
            assertThat(findSortOrder(first.getId())).isEqualTo(2);
        }

        @Test
        @DisplayName("今のタスクと合わないIDの並びを送ると、409が返り、表示順は変わらない")
        void reorder_idsNotMatchingTasks_returnsConflictAndKeepsSortOrder() {
            // 準備: 登録したときの表示順を、1ではなく10・20にする。
            // 送るのは2件目のタスクのIDだけなので、409を返す前に保存してしまう誤りがあると、2件目の表示順が1に変わり、下の確認で失敗する
            taskRepository.deleteAll();
            Task first = createTask("APIで並び替えるタスク1", 10);
            Task second = createTask("APIで並び替えるタスク2", 20);

            // 実行・確認: 一部のIDだけを送る
            assertThat(mockMvc.put().uri(contextPath + "/api/tasks/reorder")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taskIds\":[" + second.getId() + "]}"))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"送られたタスクIDが、今あるタスクのIDと一致しません\"}");

            // 確認
            clearPersistenceContext();
            assertThat(findSortOrder(first.getId())).isEqualTo(10);
            assertThat(findSortOrder(second.getId())).isEqualTo(20);
        }

        @Test
        @DisplayName("IDの並びにnullが入っていると、400ではなく409が返る")
        void reorder_nullInIds_returnsConflict() {
            // 準備
            taskRepository.deleteAll();
            Task task = createTask("APIで並び替えるタスク");

            // 実行・確認
            assertThat(mockMvc.put().uri(contextPath + "/api/tasks/reorder")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taskIds\":[" + task.getId() + ",null]}"))
                    .hasStatus(HttpStatus.CONFLICT);
        }

        @ParameterizedTest
        @ValueSource(strings = { "{}", "{\"taskIds\":null}" })
        @DisplayName("taskIdsがない場合、400と項目別のエラーが返る")
        void reorder_noTaskIds_returnsBadRequest(String body) {
            assertThat(mockMvc.put().uri(contextPath + "/api/tasks/reorder")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"入力値が不正です\",\"errors\":{\"taskIds\":\"必須入力です\"}}");
        }

        @Test
        @DisplayName("IDの並びに数値でない値があると、400と内部情報を含まないエラーが返る")
        void reorder_nonNumericId_returnsBadRequest() {
            assertThat(mockMvc.put().uri(contextPath + "/api/tasks/reorder")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taskIds\":[\"abc\"]}"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"リクエストの形式が正しくありません\"}");
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
    }

    @Nested
    @DisplayName("ステータスの変更（PATCH /api/tasks/{id}/status）")
    class UpdateStatus {
        @Test
        @DisplayName("完了に変更すると、DONEを返し、完了日時が保存される")
        void updateStatus_toDone_savesCompletedAt() {
            // 準備
            Task task = createTask("APIで完了にするタスク");

            // 実行・確認
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/" + task.getId() + "/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson("DONE")))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.status").isEqualTo(TaskStatus.DONE.name());

            // 確認: レスポンスだけでなく、データベースに保存された値も確認する
            clearPersistenceContext();
            assertThat(findStatus(task.getId())).isEqualTo(TaskStatus.DONE);
            assertThat(findCompletedAt(task.getId())).isNotNull();
        }

        @Test
        @DisplayName("完了以外へ変更すると、完了日時が解除される")
        void updateStatus_fromDone_clearsCompletedAt() {
            // 準備: 一度完了にして完了日時を持たせる
            Task task = createTask("APIで戻すタスク");
            mockMvc.patch().uri(contextPath + "/api/tasks/" + task.getId() + "/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson("DONE"))
                    .exchange();

            // 実行・確認
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/" + task.getId() + "/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson("IN_PROGRESS")))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.status").isEqualTo(TaskStatus.IN_PROGRESS.name());

            // 確認: レスポンスだけでなく、データベースに保存された値も確認する
            clearPersistenceContext();
            assertThat(findStatus(task.getId())).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(findCompletedAt(task.getId())).isNull();
        }

        @Test
        @DisplayName("ステータスが未指定の場合、400と項目別のエラーが返る")
        void updateStatus_nullStatus_returnsBadRequest() {
            // 準備
            Task task = createTask("APIで検証するタスク");

            // 実行・確認
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/" + task.getId() + "/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson(null)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors.status").isNotNull();
        }

        @Test
        @DisplayName("存在しないステータス値を送ると、400と内部情報を含まないエラーが返る")
        void updateStatus_invalidStatus_returnsBadRequest() {
            // 準備
            Task task = createTask("APIで検証するタスク");

            // 実行・確認
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/" + task.getId() + "/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson("INVALID")))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"リクエストの形式が正しくありません\"}");
        }

        @Test
        @DisplayName("ボディの形式がJSONでないと、400と内部情報を含まないエラーが返る")
        void updateStatus_nonJsonContentType_returnsBadRequest() {
            // 準備
            Task task = createTask("APIで検証するタスク");

            // 実行・確認
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/" + task.getId() + "/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(statusJson("DONE")))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"リクエストの形式が正しくありません\"}");
        }

        @Test
        @DisplayName("URLのIDが数値でない場合も、400と内部情報を含まないエラーが返る")
        void updateStatus_nonNumericId_returnsBadRequest() {
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/abc/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson("DONE")))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"リクエストの形式が正しくありません\"}");
        }

        @Test
        @DisplayName("存在しないタスクのステータスを変更しようとすると、404とIDを含まないメッセージが返る")
        void updateStatus_notFound_returnsNotFound() {
            assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/-1/status")
                    .contextPath(contextPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(statusJson("DONE")))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.message").isEqualTo("タスクが見つかりません");
        }

        /**
         * ステータス変更APIへ送るJSONを組み立てる。
         *
         * <p>
         * クライアントから送られるJSONを再現し、存在しないステータス値も送れるよう、文字列で組み立てる。
         *
         * @param status 送信するステータス値。nullを渡すとJSONのnullとして送る
         * @return リクエストボディのJSON
         */
        private String statusJson(String status) {
            return status == null
                    ? "{\"status\":null}"
                    : "{\"status\":\"" + status + "\"}";
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
    @DisplayName("削除（DELETE /api/tasks/{id}）")
    class Delete {
        @Test
        @DisplayName("削除するとデータベースから取り除かれ、204が返る")
        void delete_returnsNoContent() {
            // 準備: 削除するタスクと、消えないことを確かめるタスクを用意する
            long before = taskRepository.count();
            Task deletedTask = createTask("APIで削除するタスク");
            Task remainingTask = createTask("削除の対象外のタスク");

            // 実行・確認
            assertThat(mockMvc.delete().uri(contextPath + "/api/tasks/" + deletedTask.getId())
                    .contextPath(contextPath))
                    .hasStatus(HttpStatus.NO_CONTENT);

            // 確認: 指定したタスクだけが消え、件数は1件だけ減る
            clearPersistenceContext();
            assertThat(taskRepository.findById(deletedTask.getId())).isEmpty();
            assertThat(taskRepository.findById(remainingTask.getId())).isPresent();
            assertThat(taskRepository.count()).isEqualTo(before + 1);
        }

        @Test
        @DisplayName("存在しないタスクを削除しようとすると、404とIDを含まないメッセージが返る")
        void delete_notFound_returnsNotFound() {
            assertThat(mockMvc.delete().uri(contextPath + "/api/tasks/-1")
                    .contextPath(contextPath))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.message").isEqualTo("タスクが見つかりません");
        }

        @Test
        @DisplayName("URLのIDが数値でない場合は、400と内部情報を含まないエラーが返る")
        void delete_nonNumericId_returnsBadRequest() {
            assertThat(mockMvc.delete().uri(contextPath + "/api/tasks/abc")
                    .contextPath(contextPath))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"リクエストの形式が正しくありません\"}");
        }
    }

    /**
     * テストの準備用に、タイトルだけを指定したタスクを登録する。
     *
     * <p>
     * 表示順は、すべて同じ1で登録し、一覧の並びをIDの順（登録した順）にする。表示順を確かめるテストでは、{@link #createTask(String, int)}を使う。
     *
     * <p>
     * 検証対象のAPIを準備に使わないよう、直接保存する。
     * 以降の処理がデータベースに実際に保存された値を使うよう、保存後にデータベースから読み直す。
     *
     * @param title 登録するタイトル
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title) {
        return createTask(title, 1);
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
