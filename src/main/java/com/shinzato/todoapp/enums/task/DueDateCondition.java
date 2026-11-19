package com.shinzato.todoapp.enums.task;

/**
 * タスク一覧の、期限での絞り込みの条件。
 *
 * <p>
 * どの条件も、完了・中止のタスクを除く。
 */
public enum DueDateCondition {

    /** 今日まで。期限が今日のタスク */
    DUE_TODAY,

    /** 期限切れのみ。期限が今日より前のタスク */
    OVERDUE
}
