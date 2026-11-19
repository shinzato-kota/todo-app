package com.shinzato.todoapp.repository.task;

import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.TaskPriority;
import com.shinzato.todoapp.enums.task.TaskStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * タスクの絞り込みの条件を作る。
 *
 * <p>
 * 作った条件は、{@link TaskRepository#findAll(Specification, org.springframework.data.domain.Sort)}に渡す。
 */
public final class TaskSpecifications {

    /** キーワードの中の「%」「_」を、ただの文字として探すためのエスケープ文字 */
    private static final char LIKE_ESCAPE = '\\';

    private TaskSpecifications() {
    }

    /**
     * 指定したステータスのタスクに絞り込む条件を作る。
     *
     * @param status ステータス
     * @return 作った条件
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * 指定した優先度のタスクに絞り込む条件を作る。
     *
     * @param priority 優先度
     * @return 作った条件
     */
    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    /**
     * 期限が今日で、完了・中止以外のタスクに絞り込む条件を作る。
     *
     * @param today 今日の日付
     * @return 作った条件
     */
    public static Specification<Task> isDueToday(LocalDate today) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("dueDate"), today), isOpen(root, cb));
    }

    /**
     * 期限が今日より前で、完了・中止以外のタスクに絞り込む条件を作る。
     *
     * @param today 今日の日付
     * @return 作った条件
     */
    public static Specification<Task> isOverdue(LocalDate today) {
        return (root, query, cb) -> cb.and(cb.lessThan(root.get("dueDate"), today), isOpen(root, cb));
    }

    /**
     * タイトルか詳細に、指定した文字列が含まれるタスクに絞り込む条件を作る。
     *
     * <p>
     * 「%」「_」も、ただの文字として探す。
     *
     * @param keyword 探す文字列
     * @return 作った条件
     * @throws NullPointerException keywordがnullの場合
     */
    public static Specification<Task> containsKeyword(String keyword) {
        String pattern = "%" + escapeLike(keyword) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("title"), pattern, LIKE_ESCAPE),
                cb.like(root.get("description"), pattern, LIKE_ESCAPE));
    }

    /**
     * 指定したIDのカテゴリのタスクに絞り込む条件を作る。
     *
     * @param categoryId カテゴリID
     * @return 作った条件
     */
    public static Specification<Task> hasCategoryId(long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * カテゴリなしのタスクに絞り込む条件を作る。
     *
     * @return 作った条件
     */
    public static Specification<Task> hasNoCategory() {
        return (root, query, cb) -> cb.isNull(root.get("category"));
    }

    /**
     * 作業が終わっていない（完了・中止以外の）タスクの条件を作る。
     *
     * @param root 絞り込む対象のタスク
     * @param cb   条件を作るためのCriteriaBuilder
     * @return 作った条件
     */
    private static Predicate isOpen(Root<Task> root, CriteriaBuilder cb) {
        return root.get("status").in(TaskStatus.DONE, TaskStatus.CANCELLED).not();
    }

    /**
     * LIKEで特別な意味を持つ文字の前に、エスケープ文字を付ける。
     *
     * @param keyword 探す文字列
     * @return エスケープ文字を付けた文字列
     */
    private static String escapeLike(String keyword) {
        // エスケープ文字そのものを先に置き換える。後にすると、「%」「_」の前に付けたエスケープ文字も2重になる
        return keyword.replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}
