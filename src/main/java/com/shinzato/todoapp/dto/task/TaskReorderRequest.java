package com.shinzato.todoapp.dto.task;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 並び替えAPIが受け取るリクエストのDTO。
 *
 * @param taskIds 並び替えた後の順に並べたタスクのID
 */
public record TaskReorderRequest(@NotNull List<Long> taskIds) {
}
