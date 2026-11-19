package com.shinzato.todoapp.exception.category;

/**
 * 登録しようとしたカテゴリ名と同じ名前のカテゴリが、すでに存在する場合に発生する例外。
 */
public class DuplicateCategoryNameException extends RuntimeException {

    /**
     * 重複したカテゴリ名を含むメッセージを持つ例外を生成する。
     *
     * @param name 重複したカテゴリ名
     */
    public DuplicateCategoryNameException(String name) {
        super("同じ名前のカテゴリがあります: name=" + name);
    }
}
