package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskStatus;

/**
 * ステータス変更APIが返すレスポンスのDTO。
 *
 * @param id     タスクID
 * @param status 変更後のステータス
 */
public record TaskStatusResponse(Long id, TaskStatus status) {

    /**
     * エンティティからレスポンス用のDTOを生成する。
     *
     * @param task 変換元のタスク
     * @return 生成したDTO
     */
    public static TaskStatusResponse from(Task task) {
        return new TaskStatusResponse(task.getId(), task.getStatus());
    }
}
