package com.shinzato.todoapp.repository.task;

import com.shinzato.todoapp.entity.task.Task;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * taskテーブルへのアクセスを行うリポジトリ。
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * 条件に合うタスクを、指定した順で取得する。
     *
     * <p>
     * 一覧でカテゴリの名前と色を表示するため、各タスクのカテゴリも同じ問い合わせで読む。
     *
     * @param spec 絞り込みの条件。条件の部品は{@link TaskSpecifications}で作る
     * @param sort 並び順
     * @return 条件に合うタスク
     */
    @Override
    @EntityGraph(attributePaths = "category")
    @NonNull
    List<Task> findAll(@Nullable Specification<Task> spec, @NonNull Sort sort);

    /**
     * 表示順が最も大きいタスクを取得する。
     *
     * @return 表示順が最も大きいタスク。タスクが1件もない場合は空
     */
    Optional<Task> findFirstByOrderBySortOrderDesc();
}
