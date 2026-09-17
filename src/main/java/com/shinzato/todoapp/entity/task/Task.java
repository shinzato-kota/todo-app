package com.shinzato.todoapp.entity.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.shinzato.todoapp.enums.task.TaskStatus;

import java.time.LocalDateTime;

/**
 * taskテーブルに対応するエンティティ。
 *
 * <p>
 * データベースとのやり取りのみに使用し、画面やAPIへはDTOに詰め替えて渡す。
 */
@Entity
@Table(name = "task")
public class Task {

    /** タスクID。データベース側で自動採番する */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** タイトル */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** ステータス。未設定のまま保存されることを避けるため初期値を持たせている */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    /** 完了日時。ステータスがDONEのときのみ値を持ち、DONE以外に変更するとnullに戻す */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 作成日時。一度設定した値を変更しないため、更新対象から除外している */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新日時 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 新規登録の直前に、作成日時と更新日時へ現在日時を設定する。
     */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 更新の直前に、更新日時へ現在日時を設定する。
     */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
