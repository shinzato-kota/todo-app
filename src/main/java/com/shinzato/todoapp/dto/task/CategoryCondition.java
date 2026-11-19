package com.shinzato.todoapp.dto.task;

/**
 * タスク一覧の、カテゴリでの絞り込みの条件。
 *
 * <p>
 * 指定したIDのカテゴリ（{@link ById}）か、カテゴリなし（{@link None}）のどちらかを表す。
 */
public sealed interface CategoryCondition {

    /**
     * 指定したIDのカテゴリのタスクに絞り込む条件。
     *
     * @param id カテゴリID
     */
    record ById(long id) implements CategoryCondition {
    }

    /**
     * カテゴリなしのタスクに絞り込む条件。
     */
    record None() implements CategoryCondition {
    }
}
