package com.shinzato.todoapp.service.task;

import com.shinzato.todoapp.dto.task.TaskStatusResponse;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.exception.task.TaskNotFoundException;
import com.shinzato.todoapp.form.task.TaskForm;
import com.shinzato.todoapp.repository.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * タスクに関する業務処理を行うサービス。
 *
 * <p>
 * エンティティはこのクラスの中だけで扱い、呼び出し元へはDTOを返す。
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 一覧画面に表示するタスクを登録順で取得する。
     *
     * @return 登録順に並んだタスク一覧
     */
    @Transactional(readOnly = true)
    public List<TaskSummary> getTaskList() {
        return taskRepository.findAllByOrderByCreatedAtAscIdAsc().stream()
                .map(TaskSummary::from)
                .toList();
    }

    /**
     * 編集画面に表示するタスクを取得する。
     *
     * @param id 取得するタスクのID
     * @return 編集画面に表示するフォーム
     * @throws NullPointerException  idがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    @Transactional(readOnly = true)
    public TaskForm getTaskForEdit(Long id) {
        return TaskForm.from(getTaskOrThrow(id));
    }

    /**
     * タスクを新規登録する。ステータスは常に未着手で登録する。
     *
     * @param form 登録する内容
     */
    public void registerTask(TaskForm form) {
        Task task = new Task();
        task.setTitle(form.getTitle());
        task.setStatus(TaskStatus.TODO);
        taskRepository.save(task);
    }

    /**
     * タスクのタイトルを更新する。
     *
     * @param id   更新するタスクのID
     * @param form 更新する内容
     * @throws NullPointerException  idがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    public void updateTask(Long id, TaskForm form) {
        Task task = getTaskOrThrow(id);
        task.setTitle(form.getTitle());
    }

    /**
     * タスクのステータスを変更する。
     *
     * <p>
     * 完了に変更した場合は完了日時を設定し、完了以外へ変更した場合は完了日時を消す。
     *
     * @param id        変更するタスクのID
     * @param newStatus 変更後のステータス
     * @return 変更後のタスク
     * @throws NullPointerException  idがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    public TaskStatusResponse changeStatus(Long id, TaskStatus newStatus) {
        Task task = getTaskOrThrow(id);
        task.setStatus(newStatus);
        task.setCompletedAt(newStatus == TaskStatus.DONE ? LocalDateTime.now() : null);
        return TaskStatusResponse.from(task);
    }

    /**
     * タスクを削除する。
     *
     * @param id 削除するタスクのID
     * @throws NullPointerException  idがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    public void removeTask(Long id) {
        // 存在しないIDの削除を成功として扱わないため、取得できたものだけを削除する
        taskRepository.delete(getTaskOrThrow(id));
    }

    /**
     * IDを指定してタスクのエンティティを取得し、存在しなければ例外を投げる。
     *
     * <p>
     * エンティティをこのクラスの外へ出さないため、公開せずに内部からのみ利用する。
     *
     * @param id 取得するタスクのID
     * @return 取得したタスク
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    private Task getTaskOrThrow(Long id) {
        Objects.requireNonNull(id, "idはnullであってはいけません");
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
