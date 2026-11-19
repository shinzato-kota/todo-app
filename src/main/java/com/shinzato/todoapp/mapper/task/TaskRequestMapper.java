package com.shinzato.todoapp.mapper.task;

import com.shinzato.todoapp.dto.task.CategoryCondition;
import com.shinzato.todoapp.dto.task.TaskSearchCondition;
import com.shinzato.todoapp.dto.task.TaskSearchRequest;
import org.springframework.stereotype.Component;

/**
 * タスクのAPIのリクエストと、サービスが受け取るDTOとの間で詰め替える。
 */
@Component
public class TaskRequestMapper {

    /**
     * 絞り込みAPIの条件から、サービスに渡す絞り込みの条件を生成する。
     *
     * <p>
     * キーワードは前後の空白（全角スペースを含む）を除き、空白だけの場合は絞り込まない。
     *
     * @param request 変換元の、絞り込みのリクエスト
     * @return 生成した条件
     * @throws NullPointerException  requestがnullの場合
     * @throws NumberFormatException カテゴリが、{@value TaskSearchRequest#NO_CATEGORY}でもlongに変えられる数でもない場合
     */
    public TaskSearchCondition toSearchCondition(TaskSearchRequest request) {
        return new TaskSearchCondition(request.status(), request.due(), request.priority(),
                toKeyword(request.keyword()), toCategoryCondition(request.category()));
    }

    /**
     * キーワードの前後の空白を除く。
     *
     * @param keyword 受け取ったキーワード。送られなかった場合はnull
     * @return 前後の空白を除いたキーワード。送られなかった場合と、空白だけの場合はnull
     */
    private String toKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String stripped = keyword.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /**
     * カテゴリの条件の文字列を、カテゴリの絞り込みの条件に変える。
     *
     * @param category 受け取ったカテゴリの条件。送られなかった場合はnull
     * @return カテゴリの絞り込みの条件。送られなかった場合はnull
     */
    private CategoryCondition toCategoryCondition(String category) {
        if (category == null) {
            return null;
        }
        if (category.equals(TaskSearchRequest.NO_CATEGORY)) {
            return new CategoryCondition.None();
        }
        return new CategoryCondition.ById(Long.parseLong(category));
    }
}
