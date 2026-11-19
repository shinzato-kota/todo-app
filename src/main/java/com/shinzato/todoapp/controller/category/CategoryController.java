package com.shinzato.todoapp.controller.category;

import com.shinzato.todoapp.exception.category.DuplicateCategoryNameException;
import com.shinzato.todoapp.form.category.CategoryForm;
import com.shinzato.todoapp.mapper.category.CategoryFormMapper;
import com.shinzato.todoapp.service.category.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * カテゴリの一覧画面・追加画面の表示を担当するコントローラー。
 *
 * <p>
 * 画面遷移を伴う操作（作成）を扱い、画面遷移を伴わない操作（削除）は{@link CategoryApiController}が担当する。
 */
@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryFormMapper categoryFormMapper;

    public CategoryController(CategoryService categoryService, CategoryFormMapper categoryFormMapper) {
        this.categoryService = categoryService;
        this.categoryFormMapper = categoryFormMapper;
    }

    /**
     * カテゴリ一覧画面を表示する。
     *
     * @param model 画面へ渡すデータ
     * @return 一覧画面のテンプレート名
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.getCategoryList());
        return "categories/list";
    }

    /**
     * カテゴリ追加画面を表示する。
     *
     * @param model 画面へ渡すデータ
     * @return フォーム画面のテンプレート名
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        // @ModelAttributeの引数で受け取ると、URLのクエリパラメーターの値が入力欄に入った状態で開けてしまうため、ここで空のフォームを作る
        model.addAttribute("categoryForm", new CategoryForm());
        return "categories/form";
    }

    /**
     * カテゴリを新規登録し、一覧画面へ遷移する。
     *
     * @param form          入力内容
     * @param bindingResult 入力チェックの結果
     * @return 登録できた場合は一覧画面へのリダイレクト、入力エラーがある場合はフォーム画面
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "categories/form";
        }
        try {
            categoryService.registerCategory(categoryFormMapper.toInput(form));
        } catch (DuplicateCategoryNameException ex) {
            bindingResult.rejectValue("name", "duplicate");
            return "categories/form";
        }
        return "redirect:/categories";
    }
}
