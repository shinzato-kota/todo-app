package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.repository.task.TaskRepository;
import com.shinzato.todoapp.service.task.TaskService;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TaskController}のテスト。
 *
 * <p>
 * 実際にリクエストを送って画面の応答を確認する。
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

    @Autowired
    private MockMvcTester mockMvc;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    /** 設定値と同じコンテキストパスを使うため、プロパティから読み込む */
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Test
    @DisplayName("タイトルが空のまま保存しようとするとエラーになり、保存されない")
    void create_emptyTitle_showsErrorAndDoesNotSave() {
        // 準備: 開発用のデータが残っていても判定できるよう、保存前の件数を控える
        int before = taskService.getTaskList().size();

        // 実行・確認
        assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                .param("title", ""))
                .hasStatusOk()
                .hasViewName("tasks/form")
                .bodyText().contains("必須入力です");

        // 確認
        clearPersistenceContext();
        assertThat(taskService.getTaskList()).hasSize(before);
    }

    @Test
    @DisplayName("タイトルを入力して保存すると、一覧へ遷移して登録される")
    void create_validTitle_savesAndRedirects() {
        // 準備
        String title = "テストで登録したタスク";
        int before = taskService.getTaskList().size();

        // 実行・確認: リダイレクト先はコンテキストパスを含む形で返る
        assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                .param("title", title))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl(contextPath + "/tasks");

        // 確認
        clearPersistenceContext();
        List<TaskSummary> tasks = taskService.getTaskList();
        assertThat(tasks).hasSize(before + 1);
        assertThat(tasks.get(tasks.size() - 1).title()).isEqualTo(title);
    }

    @Test
    @DisplayName("タイトルが空白だけの場合もエラーになり、保存されない")
    void create_blankTitle_showsErrorAndDoesNotSave() {
        // 準備
        int before = taskService.getTaskList().size();

        // 実行・確認
        assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                .param("title", "   "))
                .hasStatusOk()
                .hasViewName("tasks/form")
                .bodyText().contains("必須入力です");

        // 確認
        clearPersistenceContext();
        assertThat(taskService.getTaskList()).hasSize(before);
    }

    @Test
    @DisplayName("タイトルが100文字ちょうどなら保存できる")
    void create_title100Chars_savesAndRedirects() {
        // 準備: 全角文字でもバイト数ではなく文字数で数えることを確かめるため、日本語で作る
        String title = "あ".repeat(100);
        int before = taskService.getTaskList().size();

        // 実行・確認
        assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                .param("title", title))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl(contextPath + "/tasks");

        // 確認
        clearPersistenceContext();
        List<TaskSummary> tasks = taskService.getTaskList();
        assertThat(tasks).hasSize(before + 1);
        assertThat(tasks.get(tasks.size() - 1).title()).isEqualTo(title);
    }

    @Test
    @DisplayName("タイトルが101文字だとエラーになり、保存されない")
    void create_title101Chars_showsErrorAndDoesNotSave() {
        // 準備
        int before = taskService.getTaskList().size();

        // 実行・確認
        assertThat(mockMvc.post().uri(contextPath + "/tasks").contextPath(contextPath)
                .param("title", "あ".repeat(101)))
                .hasStatusOk()
                .hasViewName("tasks/form")
                .bodyText().contains("100文字以内で入力してください");

        // 確認
        clearPersistenceContext();
        assertThat(taskService.getTaskList()).hasSize(before);
    }

    @Test
    @DisplayName("保留・中止のタスクは、表示スタイルを切り替えるためのクラスが行に付与される")
    void list_addsStatusClassToRow() {
        // 準備
        Task onHold = createTask("保留にするタスク", TaskStatus.ON_HOLD);
        Task cancelled = createTask("中止にするタスク", TaskStatus.CANCELLED);

        // 実行・確認: 開発用のデータにも同じステータスのタスクがありうるため、IDと組み合わせて確認する
        assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                .hasStatusOk()
                .bodyText()
                .contains("data-task-id=\"" + onHold.getId() + "\" class=\"status-ON_HOLD\"")
                .contains("data-task-id=\"" + cancelled.getId() + "\" class=\"status-CANCELLED\"");
    }

    @Test
    @DisplayName("タスクが0件のときは「タスクはありません」が表示される")
    void list_noTasks_showsEmptyMessage() {
        // 準備: このテストのトランザクション内で全件削除する(終了時に取り消される)
        taskRepository.deleteAll();

        // 実行・確認: メッセージは件数に関係なくHTMLに含まれ、hidden属性で表示を切り替えているため、属性を確認する
        assertThat(mockMvc.get().uri(contextPath + "/tasks").contextPath(contextPath))
                .hasStatusOk()
                .bodyText()
                .doesNotContain("class=\"empty-message\" hidden=\"hidden\"")
                .contains("id=\"task-table\" hidden=\"hidden\"");
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
    @DisplayName("存在しないタスクの編集画面を開くと、一覧へ戻される")
    void editForm_notFound_redirectsToList() {
        assertThat(mockMvc.get().uri(contextPath + "/tasks/-1/edit").contextPath(contextPath))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl(contextPath + "/tasks");
    }

    @Test
    @DisplayName("タイトルを変更して保存すると、一覧へ遷移し更新される")
    void update_validTitle_updatesAndRedirects() {
        // 準備
        Task task = createTask("変更前のタイトル");

        // 実行・確認
        assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                .param("title", "変更後のタイトル"))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl(contextPath + "/tasks");

        // 確認
        clearPersistenceContext();
        assertThat(findTitle(task.getId())).isEqualTo("変更後のタイトル");
    }

    @Test
    @DisplayName("編集画面でタイトルを空にして保存すると、エラーになり更新されない")
    void update_emptyTitle_showsErrorAndDoesNotUpdate() {
        // 準備
        Task task = createTask("変更前のタイトル");

        // 実行・確認
        assertThat(mockMvc.post().uri(contextPath + "/tasks/" + task.getId()).contextPath(contextPath)
                .param("title", ""))
                .hasStatusOk()
                .hasViewName("tasks/form")
                .bodyText().contains("必須入力です");

        // 確認
        clearPersistenceContext();
        assertThat(findTitle(task.getId())).isEqualTo("変更前のタイトル");
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
        return entityManager.persistFlushFind(task);
    }

    /**
     * データベースに保存されているタイトルを取得する。
     *
     * <p>
     * 保持中のタスクがあるとデータベースの値ではなくそちらが返るため、
     * 呼び出す前に{@link #clearPersistenceContext()}で保持を解除しておく。
     *
     * @param id 対象のタスクのID
     * @return タイトル
     */
    private String findTitle(Long id) {
        return taskRepository.findById(id).orElseThrow().getTitle();
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
