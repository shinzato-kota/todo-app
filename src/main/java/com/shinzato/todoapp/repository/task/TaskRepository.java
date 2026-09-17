package com.shinzato.todoapp.repository.task;

import com.shinzato.todoapp.entity.task.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * taskテーブルへのアクセスを行うリポジトリ。
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * 全タスクを登録順で取得する。
     *
     * @return 作成日時の昇順、同じ場合はIDの昇順に並んだ全タスク
     */
    List<Task> findAllByOrderByCreatedAtAscIdAsc();
}
