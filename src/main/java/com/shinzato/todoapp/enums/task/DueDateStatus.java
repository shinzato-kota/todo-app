package com.shinzato.todoapp.enums.task;

/**
 * タスクの期限の状況。
 *
 * <p>
 * データベースには保存せず、画面やAPIへ返すたびにサービスで判定する。
 */
public enum DueDateStatus {

    /** 期限切れ。期限が今日より前で、完了・中止以外 */
    OVERDUE,

    /** 今日が期限。ステータスは問わない */
    DUE_TODAY,

    /** 期限切れでも今日が期限でもない。期限なし、期限が明日以降、または完了・中止で期限が過ぎている */
    NORMAL
}
