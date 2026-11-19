package com.shinzato.todoapp.enums.task;

/**
 * タスクの優先度。
 *
 * <p>
 * データベースには定数名を文字列として保存し、画面には日本語の表示名を出す。
 */
public enum TaskPriority {

    /** 高 */
    HIGH("高"),

    /** 中。未設定時の既定値 */
    MEDIUM("中"),

    /** 低 */
    LOW("低");

    /** 画面に表示する日本語名 */
    private final String displayName;

    TaskPriority(String displayName) {
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
