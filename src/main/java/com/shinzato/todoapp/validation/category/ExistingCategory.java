package com.shinzato.todoapp.validation.category;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 存在するカテゴリのIDであることを確かめる入力チェック。{@code Long}型のフィールドに付ける。
 *
 * <p>
 * 判定は{@link ExistingCategoryValidator}が行う。
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExistingCategoryValidator.class)
public @interface ExistingCategory {

    String message() default "{com.shinzato.todoapp.validation.category.ExistingCategory.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
