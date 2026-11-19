package com.shinzato.todoapp.dto.category;

/**
 * カテゴリの登録で、サービスが受け取る入力のDTO。
 *
 * @param name  カテゴリ名
 * @param color ラベル色。#rrggbb形式で、色なしの場合はnull
 */
public record CategoryInput(String name, String color) {
}
