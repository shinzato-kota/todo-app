package com.shinzato.todoapp.controller.category;

import com.shinzato.todoapp.service.category.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * カテゴリのAPIを担当するコントローラー。
 *
 * <p>
 * 画面を再読み込みせずに反映したい操作をJSONで扱い、
 * 画面遷移を伴う操作は{@link CategoryController}が担当する。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * カテゴリを削除する。このカテゴリに属していたタスクは「カテゴリなし」になる。
     *
     * @param id 削除するカテゴリのID
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.removeCategory(id);
        return ResponseEntity.noContent().build();
    }
}
