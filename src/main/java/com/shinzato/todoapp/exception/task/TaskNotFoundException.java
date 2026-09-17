package com.shinzato.todoapp.exception.task;

/**
 * 指定したIDのタスクが存在しない場合に発生する例外。
 */
public class TaskNotFoundException extends RuntimeException {

    /**
     * 見つからなかったタスクのIDを含むメッセージを持つ例外を生成する。
     *
     * @param id 見つからなかったタスクのID
     */
    public TaskNotFoundException(Long id) {
        super("タスクが見つかりません: id=" + id);
    }
}
