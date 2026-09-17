package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.dto.task.TaskStatusResponse;
import com.shinzato.todoapp.dto.task.TaskStatusUpdateRequest;
import com.shinzato.todoapp.service.task.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * タスクのAPIを担当するコントローラー。
 *
 * <p>
 * 画面を再読み込みせずに反映したい操作をJSONで扱い、
 * 画面遷移を伴う操作は{@link TaskController}が担当する。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private final TaskService taskService;

    public TaskApiController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * タスクのステータスを変更する。
     *
     * <p>
     * 一部の項目だけを更新するため、HTTPメソッドはPATCHを使用する。
     *
     * @param id      変更するタスクのID
     * @param request 変更後のステータス
     * @return 200と、変更後のステータス
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskStatusResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(id, request.status()));
    }

    /**
     * タスクを削除する。
     *
     * @param id 削除するタスクのID
     * @return 204。削除後に返す内容がないためボディは持たない
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.removeTask(id);
        return ResponseEntity.noContent().build();
    }
}
