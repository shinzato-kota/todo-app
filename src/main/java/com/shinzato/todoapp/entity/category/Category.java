package com.shinzato.todoapp.entity.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * categoryテーブルに対応するエンティティ。
 *
 * <p>
 * データベースとのやり取りのみに使用し、画面やAPIへはDTOに詰め替えて渡す。
 */
@Entity
@Table(name = "category")
public class Category {

    /** カテゴリID。データベース側で自動採番する */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** カテゴリ名。同じ名前は登録できない */
    @Column(name = "name", nullable = false, length = 20, unique = true)
    private String name;

    /** ラベル色。#RRGGBB形式。未設定の場合はnull */
    @Column(name = "color", length = 7)
    private String color;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
