package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.form.task.TaskForm;
import com.shinzato.todoapp.service.task.TaskService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * タスクの画面表示を担当するコントローラー。
 *
 * <p>
 * 画面遷移を伴う操作を扱い、画面遷移を伴わない操作は{@link TaskApiController}が担当する。
 */
@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * タスク一覧画面を表示する。
     *
     * @param model 画面へ渡すデータ
     * @return 一覧画面のテンプレート名
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("tasks", taskService.getTaskList());
        // ステータス変更のプルダウンで選択肢として使うため、全ステータスを渡す
        model.addAttribute("statuses", TaskStatus.values());
        return "tasks/list";
    }

    /**
     * タスク追加画面を表示する。
     *
     * @param model 画面へ渡すデータ
     * @return フォーム画面のテンプレート名
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("task", new TaskForm());
        return "tasks/form";
    }

    /**
     * タスク編集画面を表示する。
     *
     * @param id    編集するタスクのID
     * @param model 画面へ渡すデータ
     * @return フォーム画面のテンプレート名
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("task", taskService.getTaskForEdit(id));
        return "tasks/form";
    }

    /**
     * タスクを新規登録し、一覧画面へ遷移する。
     *
     * @param form          入力内容
     * @param bindingResult 入力チェックの結果
     * @return 登録できた場合は一覧画面へのリダイレクト、入力エラーがある場合はフォーム画面
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("task") TaskForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tasks/form";
        }
        taskService.registerTask(form);
        return "redirect:/tasks";
    }

    /**
     * タスクを更新し、一覧画面へ遷移する。
     *
     * @param id            更新するタスクのID
     * @param form          入力内容
     * @param bindingResult 入力チェックの結果
     * @return 更新できた場合は一覧画面へのリダイレクト、入力エラーがある場合はフォーム画面
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute("task") TaskForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "tasks/form";
        }
        taskService.updateTask(id, form);
        return "redirect:/tasks";
    }
}
