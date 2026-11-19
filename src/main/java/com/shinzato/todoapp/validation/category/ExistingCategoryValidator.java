package com.shinzato.todoapp.validation.category;

import com.shinzato.todoapp.service.category.CategoryService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ExistingCategory}の判定を行う。
 */
public class ExistingCategoryValidator implements ConstraintValidator<ExistingCategory, Long> {

    private final CategoryService categoryService;

    public ExistingCategoryValidator(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * カテゴリのIDが、存在するカテゴリのものかどうかを判定する。
     *
     * @param value   判定するカテゴリのID。カテゴリなしの場合はnull
     * @param context 判定の状態
     * @return 存在するカテゴリのIDか、nullであればtrue
     */
    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return categoryService.existsCategory(value);
    }
}
