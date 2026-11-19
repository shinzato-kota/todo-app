package com.shinzato.todoapp.service.task;

import com.shinzato.todoapp.dto.task.CategoryCondition;
import com.shinzato.todoapp.dto.task.TaskEditValues;
import com.shinzato.todoapp.dto.task.TaskInput;
import com.shinzato.todoapp.dto.task.TaskSearchCondition;
import com.shinzato.todoapp.dto.task.TaskStatusChangeResult;
import com.shinzato.todoapp.dto.task.TaskSummary;
import com.shinzato.todoapp.entity.category.Category;
import com.shinzato.todoapp.entity.task.Task;
import com.shinzato.todoapp.enums.task.DueDateStatus;
import com.shinzato.todoapp.enums.task.TaskStatus;
import com.shinzato.todoapp.exception.category.CategoryNotFoundException;
import com.shinzato.todoapp.exception.task.TaskNotFoundException;
import com.shinzato.todoapp.exception.task.TaskOrderConflictException;
import com.shinzato.todoapp.mapper.task.TaskEntityMapper;
import com.shinzato.todoapp.repository.category.CategoryRepository;
import com.shinzato.todoapp.repository.task.TaskRepository;
import com.shinzato.todoapp.repository.task.TaskSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * タスクに関する業務処理を行うサービス。
 *
 * <p>
 * エンティティはこのクラスの中だけで扱い、呼び出し元へはDTOを返す。
 */
@Service
@Transactional
public class TaskService {

    /** 一覧の並び順。表示順の昇順で、同じ場合はIDの昇順 */
    private static final Sort LIST_ORDER = Sort.by("sortOrder", "id");

    /** 絞り込まない条件 */
    private static final TaskSearchCondition NO_CONDITION = new TaskSearchCondition(null, null, null, null, null);

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final TaskEntityMapper taskEntityMapper;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, CategoryRepository categoryRepository,
            TaskEntityMapper taskEntityMapper, Clock clock) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.taskEntityMapper = taskEntityMapper;
        this.clock = clock;
    }

    /**
     * 一覧画面に表示するタスクを表示順で取得する。
     *
     * @return 表示順に並んだタスク一覧。各件の期限の状況は、今日の日付で判定したもの
     */
    @Transactional(readOnly = true)
    public List<TaskSummary> getTaskList() {
        // 最初の表示と、絞り込みの「すべて」の結果が食い違わないよう、同じ問い合わせで読む
        return searchTasks(NO_CONDITION);
    }

    /**
     * 条件に合うタスクを表示順で取得する。
     *
     * @param condition 絞り込みの条件
     * @return 条件に合うタスクを表示順に並べた一覧。各件の期限の状況は、今日の日付で判定したもの
     * @throws NullPointerException conditionがnullの場合
     */
    @Transactional(readOnly = true)
    public List<TaskSummary> searchTasks(TaskSearchCondition condition) {
        // 絞り込みと期限の状況の判定の途中で日付が変わっても同じ日付を使うよう、今日の日付を先に1回だけ求める
        LocalDate today = LocalDate.now(clock);
        return taskRepository.findAll(toSpecification(condition, today), LIST_ORDER).stream()
                .map(task -> taskEntityMapper.toSummary(task, judgeDueDateStatus(task, today)))
                .toList();
    }

    /**
     * 絞り込みの条件のうち、指定された条件だけをつないだ問い合わせの条件を作る。
     *
     * @param condition 絞り込みの条件
     * @param today     今日の日付
     * @return 問い合わせの条件。指定された条件がない場合は、絞り込まない条件
     */
    private Specification<Task> toSpecification(TaskSearchCondition condition, LocalDate today) {
        List<Specification<Task>> specs = new ArrayList<>();
        if (condition.status() != null) {
            specs.add(TaskSpecifications.hasStatus(condition.status()));
        }
        if (condition.due() != null) {
            specs.add(switch (condition.due()) {
                case DUE_TODAY -> TaskSpecifications.isDueToday(today);
                case OVERDUE -> TaskSpecifications.isOverdue(today);
            });
        }
        if (condition.priority() != null) {
            specs.add(TaskSpecifications.hasPriority(condition.priority()));
        }
        if (condition.keyword() != null) {
            specs.add(TaskSpecifications.containsKeyword(condition.keyword()));
        }
        if (condition.category() != null) {
            specs.add(switch (condition.category()) {
                case CategoryCondition.ById byId -> TaskSpecifications.hasCategoryId(byId.id());
                case CategoryCondition.None none -> TaskSpecifications.hasNoCategory();
            });
        }
        return Specification.allOf(specs);
    }

    /**
     * 期限と今日の日付から、タスクの期限の状況を判定する。
     *
     * <p>
     * 今日が期限であればステータスを問わない。期限切れは、完了・中止のタスクを除く。
     *
     * @param task  判定するタスク
     * @param today 今日の日付
     * @return 期限の状況
     */
    private DueDateStatus judgeDueDateStatus(Task task, LocalDate today) {
        LocalDate dueDate = task.getDueDate();
        if (dueDate == null) {
            return DueDateStatus.NORMAL;
        }
        if (today.isEqual(dueDate)) {
            return DueDateStatus.DUE_TODAY;
        }
        if (today.isAfter(dueDate) && !isClosed(task.getStatus())) {
            return DueDateStatus.OVERDUE;
        }
        return DueDateStatus.NORMAL;
    }

    /**
     * 作業が終わっている（完了・中止の）ステータスかどうかを返す。
     *
     * @param status 判定するステータス
     * @return 完了・中止であればtrue
     */
    private boolean isClosed(TaskStatus status) {
        return status == TaskStatus.DONE || status == TaskStatus.CANCELLED;
    }

    /**
     * 編集画面に表示するタスクを取得する。
     *
     * @param id 取得するタスクのID
     * @return 編集画面に表示する項目
     * @throws NullPointerException  idがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    @Transactional(readOnly = true)
    public TaskEditValues getTaskEditValues(Long id) {
        return taskEntityMapper.toEditValues(getTaskOrThrow(id));
    }

    /**
     * タスクを新規登録する。ステータスは常に未着手にし、表示順は今あるどのタスクよりも後にする。
     *
     * @param input 登録する内容
     * @throws NullPointerException      inputがnullの場合
     * @throws CategoryNotFoundException 選んだカテゴリが存在しない場合
     */
    public void registerTask(TaskInput input) {
        Task task = new Task();
        taskEntityMapper.copyInputToTask(input, findCategoryOrNull(input.categoryId()), task);
        task.setStatus(TaskStatus.TODO);
        task.setSortOrder(maxSortOrderPlusOne());
        taskRepository.save(task);
    }

    /**
     * IDを指定してカテゴリのエンティティを取得する。
     *
     * @param categoryId 取得するカテゴリのID。カテゴリなしの場合はnull
     * @return 取得したカテゴリ。IDがnullの場合はnull
     * @throws CategoryNotFoundException 指定したIDのカテゴリが存在しない場合
     */
    private Category findCategoryOrNull(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    /**
     * 今ある表示順の最大値に1を足した値を返す。
     *
     * @return 表示順の最大値に1を足した値。タスクが1件もない場合は1
     */
    private int maxSortOrderPlusOne() {
        return taskRepository.findFirstByOrderBySortOrderDesc()
                .map(Task::getSortOrder)
                .orElse(0) + 1;
    }

    /**
     * タスクのタイトル・詳細・期限・優先度・カテゴリを更新する。
     *
     * @param id    更新するタスクのID
     * @param input 更新する内容
     * @throws NullPointerException      idまたはinputがnullの場合
     * @throws TaskNotFoundException     指定したIDのタスクが存在しない場合
     * @throws CategoryNotFoundException 選んだカテゴリが存在しない場合
     */
    public void updateTask(Long id, TaskInput input) {
        Task task = getTaskOrThrow(id);
        taskEntityMapper.copyInputToTask(input, findCategoryOrNull(input.categoryId()), task);
    }

    /**
     * タスクのステータスを変更する。
     *
     * <p>
     * 完了以外から完了に変更した場合は完了日時を設定し、完了以外へ変更した場合は完了日時を消す。
     * すでに完了のタスクを完了に変更した場合は、完了日時を変えない。
     *
     * @param id        変更するタスクのID
     * @param newStatus 変更後のステータス
     * @return 変更後のステータスと、変更後のステータスで判定した期限の状況
     * @throws NullPointerException  idまたはnewStatusがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    public TaskStatusChangeResult changeStatus(Long id, TaskStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatusはnullであってはいけません");
        Task task = getTaskOrThrow(id);
        if (newStatus != TaskStatus.DONE) {
            task.setCompletedAt(null);
        } else if (task.getStatus() != TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task.setStatus(newStatus);
        return taskEntityMapper.toStatusChangeResult(task, judgeDueDateStatus(task, LocalDate.now(clock)));
    }

    /**
     * タスクの表示順を、送られたIDの並びのとおりに変える。先頭のタスクの表示順を1にする。
     *
     * @param taskIds 並び替えた後のタスクのID。今あるタスクのIDをすべて1回ずつ含む
     * @throws NullPointerException       taskIdsがnullの場合
     * @throws TaskOrderConflictException taskIdsが、今あるタスクのIDをすべて1回ずつ含む形でない場合
     */
    public void reorderTasks(List<Long> taskIds) {
        Map<Long, Task> tasksById = taskRepository.findAll().stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));
        // 重複があると、件数だけでも集合だけでも見逃す場合があるため、両方を比べる
        if (taskIds.size() != tasksById.size() || !new HashSet<>(taskIds).equals(tasksById.keySet())) {
            throw new TaskOrderConflictException();
        }
        // 読み込んだタスクへの代入なら、値が変わったタスクだけがUPDATEされ、更新日時もそのタスクだけ変わる
        for (int i = 0; i < taskIds.size(); i++) {
            tasksById.get(taskIds.get(i)).setSortOrder(i + 1);
        }
    }

    /**
     * タスクを削除する。
     *
     * @param id 削除するタスクのID
     * @throws NullPointerException  idがnullの場合
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    public void removeTask(Long id) {
        // 存在しないIDの削除を成功として扱わないため、取得できたものだけを削除する
        taskRepository.delete(getTaskOrThrow(id));
    }

    /**
     * IDを指定してタスクのエンティティを取得し、存在しなければ例外を投げる。
     *
     * <p>
     * エンティティをこのクラスの外へ出さないため、公開せずに内部からのみ利用する。
     *
     * @param id 取得するタスクのID
     * @return 取得したタスク
     * @throws TaskNotFoundException 指定したIDのタスクが存在しない場合
     */
    private Task getTaskOrThrow(Long id) {
        Objects.requireNonNull(id, "idはnullであってはいけません");
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
