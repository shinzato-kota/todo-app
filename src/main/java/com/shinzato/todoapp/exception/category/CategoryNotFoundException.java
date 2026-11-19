package com.shinzato.todoapp.exception.category;

/**
 * 指定したIDのカテゴリが存在しない場合に発生する例外。
 */
public class CategoryNotFoundException extends RuntimeException {

    /**
     * 見つからなかったカテゴリのIDを含むメッセージを持つ例外を生成する。
     *
     * @param id 見つからなかったカテゴリのID
     */
    public CategoryNotFoundException(Long id) {
        super("カテゴリが見つかりません: id=" + id);
    }
}
