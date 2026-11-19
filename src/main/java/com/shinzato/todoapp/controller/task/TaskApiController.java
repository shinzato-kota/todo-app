package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.dto.task.TaskReorderRequest;
import com.shinzato.todoapp.dto.task.TaskSearchRequest;
import com.shinzato.todoapp.dto.task.TaskStatusChangeResult;
import com.shinzato.todoapp.dto.task.TaskStatusUpdateRequest;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.mapper.task.TaskRequestMapper;
import com.shinzato.todoapp.service.task.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final TaskRequestMapper taskRequestMapper;

    public TaskApiController(TaskService taskService, TaskRequestMapper taskRequestMapper) {
        this.taskService = taskService;
        this.taskRequestMapper = taskRequestMapper;
    }

    /**
     * 条件に合うタスクを表示順で取得する。
     *
     * @param request 絞り込みの条件。送られなかった条件では絞り込まない
     * @return 200と、条件に合うタスクの配列
     */
    @GetMapping
    public ResponseEntity<List<TaskSummary>> search(@Valid TaskSearchRequest request) {
        return ResponseEntity.ok(taskService.searchTasks(taskRequestMapper.toSearchCondition(request)));
    }

    /**
     * タスクの表示順を、送られたIDの並びのとおりに変える。
     *
     * @param request 並び替えた後のタスクのID
     * @return 204
     */
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@Valid @RequestBody TaskReorderRequest request) {
        taskService.reorderTasks(request.taskIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * タスクのステータスを変更する。
     *
     * @param id      変更するタスクのID
     * @param request 変更後のステータス
     * @return 200と、ステータスを変更した結果（ID・変更後のステータス・期限の状況）
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskStatusChangeResult> updateStatus(@PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(id, request.status()));
    }

    /**
     * タスクを削除する。
     *
     * @param id 削除するタスクのID
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.removeTask(id);
        return ResponseEntity.noContent().build();
    }
}
