package com.shinzato.todoapp.dto.task;

import com.shinzato.todoapp.enums.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * ステータス変更APIが受け取るリクエストのDTO。
 *
 * @param status 変更後のステータス
 */
public record TaskStatusUpdateRequest(@NotNull TaskStatus status) {
}
