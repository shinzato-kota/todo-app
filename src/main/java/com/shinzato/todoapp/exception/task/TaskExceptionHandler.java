package com.shinzato.todoapp.exception.task;

import com.shinzato.todoapp.controller.task.TaskController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 画面用の例外ハンドラー。
 *
 * <p>
 * APIはJSONで応答するため、対象を{@link TaskController}に限定している。
 */
@ControllerAdvice(assignableTypes = TaskController.class)
public class TaskExceptionHandler {

    /**
     * 対象のタスクが存在しない場合に、一覧画面へ戻す。
     *
     * <p>
     * 一覧を開き直せば現在存在するタスクが分かるため、専用のエラー画面は用意しない。
     *
     * @return 一覧画面へのリダイレクト
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public String handleTaskNotFound() {
        return "redirect:/tasks";
    }
}
