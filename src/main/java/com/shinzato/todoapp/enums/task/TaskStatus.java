package com.shinzato.todoapp.enums.task;

/**
 * タスクのステータス。
 *
 * <p>
 * データベースには定数名を文字列として保存し、画面には日本語の表示名を出す。
 */
public enum TaskStatus {

    /** 未着手。新規作成時の初期値 */
    TODO("未着手"),

    /** 進行中。着手済みで未完了 */
    IN_PROGRESS("進行中"),

    /** 保留。一時的に作業を止めている */
    ON_HOLD("保留"),

    /** 完了 */
    DONE("完了"),

    /** 中止。実施しないことになった */
    CANCELLED("中止");

    /** 画面に表示する日本語名 */
    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 画面表示用の日本語名を返す。
     *
     * @return 日本語の表示名
     */
    public String getDisplayName() {
        return displayName;
    }
}
