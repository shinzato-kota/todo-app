package com.shinzato.todoapp.form.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * カテゴリ作成フォームで扱うフォームオブジェクト。
 */
public class CategoryForm {

    /** カテゴリ名 */
    @NotBlank
    @Size(max = 20)
    private String name;

    /**
     * ラベル色。#rrggbb形式
     *
     * <p>
     * 「色を付けない」を選んだ場合も、ブラウザーは色の欄の値を必ず送るため、形式は常に確かめる。
     */
    @NotNull
    @Pattern(regexp = "#[0-9a-f]{6}")
    private String color;

    /** 「色を付けない」を選んだ場合はtrue */
    private boolean noColor;

    public String getName() {
        return name;
    }

    /**
     * カテゴリ名を設定する。
     *
     * <p>
     * 前後の空白（全角スペースを含む）を削る。
     *
     * @param name カテゴリ名
     */
    public void setName(String name) {
        // 前後の空白だけが違う、見た目が同じ別の名前ができないようにする
        this.name = (name == null) ? null : name.strip();
    }

    public String getColor() {
        return color;
    }

    /**
     * ラベル色を設定する。
     *
     * <p>
     * 英字を小文字にそろえる。
     *
     * @param color ラベル色
     */
    public void setColor(String color) {
        this.color = (color == null) ? null : color.toLowerCase(Locale.ROOT);
    }

    public boolean isNoColor() {
        return noColor;
    }

    public void setNoColor(boolean noColor) {
        this.noColor = noColor;
    }
}
