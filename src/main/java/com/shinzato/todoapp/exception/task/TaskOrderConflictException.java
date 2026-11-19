package com.shinzato.todoapp.exception.task;

/**
 * 並び替えで送られたIDの並びが、今あるタスクのIDをすべて1回ずつ含む形でない場合に発生する例外。
 */
public class TaskOrderConflictException extends RuntimeException {

    /**
     * 固定のメッセージを持つ例外を生成する。
     */
    public TaskOrderConflictException() {
        super("送られたIDの並びが、今あるタスクのIDをすべて1回ずつ含む形ではありません");
    }
}
