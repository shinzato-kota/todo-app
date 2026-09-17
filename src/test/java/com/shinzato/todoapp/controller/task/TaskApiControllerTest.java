package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.repository.task.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * jQueryから呼び出すAPIと同じ形式でリクエストを送り、応答を確認する。
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

        // 確認: 応答だけでなく、データベースに保存された値も確認する
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

        // 確認: 応答だけでなく、データベースに保存された値も確認する
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
                .bodyJson().extractingPath("$.message").isEqualTo("リクエストの形式が正しくありません");
    }

    @Test
    @DisplayName("URLのIDが数値でない場合も、400と内部情報を含まないエラーが返る")
    void updateStatus_nonNumericId_returnsBadRequest() {
        assertThat(mockMvc.patch().uri(contextPath + "/api/tasks/abc/status")
                .contextPath(contextPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusJson("DONE")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.message").isEqualTo("リクエストの形式が正しくありません");
    }

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
    @DisplayName("存在しないタスクを削除しようとすると、404とメッセージが返る")
    void delete_notFound_returnsNotFound() {
        assertThat(mockMvc.delete().uri(contextPath + "/api/tasks/-1")
                .contextPath(contextPath))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.message").isNotNull();
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
     * テストの準備用に、タイトルだけを指定したタスクを登録する。
     *
     * <p>
     * 検証対象のAPIを準備に使わないよう、直接保存する。
     * 以降の処理がデータベースに実際に保存された値を使うよう、保存後にデータベースから読み直す。
     *
     * @param title 登録するタイトル
     * @return データベースから読み直したタスク
     */
    private Task createTask(String title) {
        Task task = new Task();
        task.setTitle(title);
        return entityManager.persistFlushFind(task);
    }

    /**
     * データベースに保存されているステータスを取得する。
     *
     * <p>
     * 保持中のタスクがあるとデータベースの値ではなくそちらが返るため、
     * 呼び出す前に{@link #clearPersistenceContext()}で保持を解除しておく。
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
     * 保持中のタスクがあるとデータベースの値ではなくそちらが返るため、
     * 呼び出す前に{@link #clearPersistenceContext()}で保持を解除しておく。
     *
     * @param id 対象のタスクのID
     * @return 完了日時。設定されていない場合はnull
     */
    private LocalDateTime findCompletedAt(Long id) {
        return taskRepository.findById(id).orElseThrow().getCompletedAt();
    }

    /**
     * 保持中の変更をデータベースへ反映し、保持を解除する。
     *
     * <p>
     * 取得時に保持中のタスクではなくデータベースに保存された値を読むため、確認の前に呼び出す。
     */
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
