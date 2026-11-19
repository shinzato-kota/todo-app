package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.enums.task.DueDateStatus;
import com.shinzato.todoapp.enums.task.TaskStatus;

/**
 * タスクのステータスを変更した結果を持つDTO。ステータス変更APIのレスポンスにも、そのまま使う。
 *
 * @param id            タスクID
 * @param status        変更後のステータス
 * @param dueDateStatus 変更後のステータスで判定した、期限の状況
 */
public record TaskStatusChangeResult(Long id, TaskStatus status, DueDateStatus dueDateStatus) {
}
