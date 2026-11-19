package com.shinzato.todoapp.exception.task;

import com.shinzato.todoapp.controller.task.TaskController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 画面用の例外ハンドラー。
 *
 * <p>
 * APIはJSONのレスポンスを返すため、対象を{@link TaskController}に限定している。
 */
@ControllerAdvice(assignableTypes = TaskController.class)
public class TaskExceptionHandler {

    /**
     * 対象のタスクが存在しない場合に、一覧画面へ戻し、タスクが見つからないことを知らせる。
     *
     * <p>
     * 一覧を開き直せば現在存在するタスクが分かるため、専用のエラー画面は用意しない。
     *
     * @param redirectAttributes 一覧画面へ渡す知らせの文
     * @return 一覧画面へのリダイレクト
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public String handleTaskNotFound(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("alertMessage", "タスクが見つかりません。");
        return "redirect:/tasks";
    }
}
