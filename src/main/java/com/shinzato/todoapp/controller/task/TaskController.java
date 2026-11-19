package com.shinzato.todoapp.controller.task;

import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.form.task.TaskForm;
import com.shinzato.todoapp.mapper.task.TaskFormMapper;
import com.shinzato.todoapp.service.category.CategoryService;
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
    private final CategoryService categoryService;
    private final TaskFormMapper taskFormMapper;

    public TaskController(TaskService taskService, CategoryService categoryService, TaskFormMapper taskFormMapper) {
        this.taskService = taskService;
        this.categoryService = categoryService;
        this.taskFormMapper = taskFormMapper;
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
        // ステータス変更のプルダウンと、絞り込みのボタンで選択肢として使うため、全ステータスを渡す
        model.addAttribute("statuses", TaskStatus.values());
        // 絞り込みのボタンと、絞り込んだ後に行を作るときに複製する優先度のバッジのテンプレートに使う
        model.addAttribute("priorities", TaskPriority.values());
        // 絞り込みのカテゴリのプルダウンで選択肢として使う
        model.addAttribute("categories", categoryService.getCategoryList());
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
        TaskForm form = new TaskForm();
        // 追加画面では、初めから「中」を選んだ状態でプルダウンを表示する
        form.setPriority(TaskPriority.MEDIUM);
        model.addAttribute("taskForm", form);
        addSelectOptions(model);
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
        model.addAttribute("taskForm", taskFormMapper.toForm(taskService.getTaskEditValues(id)));
        addSelectOptions(model);
        return "tasks/form";
    }

    /**
     * タスクを新規登録し、一覧画面へ遷移する。
     *
     * @param form          入力内容
     * @param bindingResult 入力チェックの結果
     * @param model         画面へ渡すデータ
     * @return 登録できた場合は一覧画面へのリダイレクト、入力エラーがある場合はフォーム画面
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("taskForm") TaskForm form, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addSelectOptions(model);
            return "tasks/form";
        }
        taskService.registerTask(taskFormMapper.toInput(form));
        return "redirect:/tasks";
    }

    /**
     * タスクを更新し、一覧画面へ遷移する。
     *
     * @param id            更新するタスクのID
     * @param form          入力内容
     * @param bindingResult 入力チェックの結果
     * @param model         画面へ渡すデータ
     * @return 更新できた場合は一覧画面へのリダイレクト、入力エラーがある場合はフォーム画面
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute("taskForm") TaskForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addSelectOptions(model);
            return "tasks/form";
        }
        taskService.updateTask(id, taskFormMapper.toInput(form));
        return "redirect:/tasks";
    }

    /**
     * 追加・編集画面のプルダウンで選択肢として使う値を、画面へ渡す。
     *
     * @param model 画面へ渡すデータ
     */
    private void addSelectOptions(Model model) {
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("categories", categoryService.getCategoryList());
    }
}
