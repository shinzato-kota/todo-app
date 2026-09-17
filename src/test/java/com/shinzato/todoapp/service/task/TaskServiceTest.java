package com.shinzato.todoapp.service.task;

import com.shinzato.todoapp.dto.task.TaskStatusResponse;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.exception.task.TaskNotFoundException;
import com.shinzato.todoapp.form.task.TaskForm;
import com.shinzato.todoapp.repository.task.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TaskService}のテスト。
 *
 * <p>
 * 開発用のデータベースに接続して確認するため、テストでの変更が残らないよう@Transactionalを付けている。
 */
@SpringBootTest
@AutoConfigureTestEntityManager
@Transactional
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("タスクを登録すると、ステータスが未着手になる")
    void registerTask_setsStatusToTodo() {
        // 準備: 開発用のデータが残っていても判定できるよう、登録前の件数を控える
        String title = "買い物に行く";
        TaskForm form = new TaskForm();
        form.setTitle(title);
        int before = taskService.getTaskList().size();

        // 実行
        taskService.registerTask(form);

        // 確認
        clearPersistenceContext();
        List<TaskSummary> tasks = taskService.getTaskList();
        assertThat(tasks).hasSize(before + 1);
        TaskSummary registered = tasks.get(tasks.size() - 1);
        assertThat(registered.title()).isEqualTo(title);
        assertThat(registered.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    @DisplayName("ステータスを完了に変更すると、完了日時が設定される")
    void changeStatus_toDone_setsCompletedAt() {
        // 準備
        Task created = createTask("完了にするタスク");

        // 実行
        TaskStatusResponse updated = taskService.changeStatus(created.getId(), TaskStatus.DONE);

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
        TaskStatusResponse updated = taskService.changeStatus(created.getId(), TaskStatus.IN_PROGRESS);

        // 確認: 戻り値だけでなく、データベースに保存された値も確認する
        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        clearPersistenceContext();
        assertThat(findStatus(created.getId())).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(findCompletedAt(created.getId())).isNull();
    }

    @Test
    @DisplayName("保留へ変更でき、完了日時は設定されない")
    void changeStatus_toOnHold_doesNotSetCompletedAt() {
        // 準備
        Task created = createTask("保留にするタスク");

        // 実行
        TaskStatusResponse updated = taskService.changeStatus(created.getId(), TaskStatus.ON_HOLD);

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
        TaskStatusResponse updated = taskService.changeStatus(created.getId(), TaskStatus.CANCELLED);

        // 確認: 戻り値だけでなく、データベースに保存された値も確認する
        assertThat(updated.status()).isEqualTo(TaskStatus.CANCELLED);
        clearPersistenceContext();
        assertThat(findStatus(created.getId())).isEqualTo(TaskStatus.CANCELLED);
        assertThat(findCompletedAt(created.getId())).isNull();
    }

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

    @Test
    @DisplayName("タスクのタイトルを更新できる")
    void updateTask_changesTitle() {
        // 準備
        String updatedTitle = "変更後のタイトル";
        Task created = createTask("変更前のタイトル");
        TaskForm form = new TaskForm();
        form.setTitle(updatedTitle);

        // 実行
        taskService.updateTask(created.getId(), form);

        // 確認
        clearPersistenceContext();
        assertThat(taskRepository.findById(created.getId()).orElseThrow().getTitle())
                .isEqualTo(updatedTitle);
    }

    @Test
    @DisplayName("一覧は登録した順に並ぶ")
    void getTaskList_ordersByRegistration() {
        // 準備
        String firstTitle = "先に登録するタスク";
        String secondTitle = "後に登録するタスク";
        createTask(firstTitle);
        createTask(secondTitle);

        // 実行
        List<TaskSummary> tasks = taskService.getTaskList();

        // 確認: 登録した2件がこの順で現れる
        assertThat(tasks).extracting(TaskSummary::title)
                .containsSubsequence(firstTitle, secondTitle);
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
