package com.shinzato.todoapp.dto.category;

/**
 * カテゴリ1件分の表示項目を持つDTO。
 *
 * @param id    カテゴリID
 * @param name  カテゴリ名
 * @param color ラベル色。#rrggbb形式。未設定の場合はnull
 */
public record CategorySummary(Long id, String name, String color) {
}
