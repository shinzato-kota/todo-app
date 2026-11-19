-- データベース・テーブル作成(DBファースト方式)
-- 新しく環境を作るときに実行する。フェーズ1の環境を上げる場合は migration/phase2_upgrade_01.sql から順に実行する
CREATE DATABASE IF NOT EXISTS tododb CHARACTER
SET
  utf8mb4 COLLATE utf8mb4_general_ci;

USE tododb;

CREATE TABLE category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'カテゴリID',
  name VARCHAR(20) NOT NULL COMMENT 'カテゴリ名',
  color VARCHAR(7) NULL COMMENT 'ラベル色',
  CONSTRAINT uk_category_name UNIQUE (name)
) COMMENT = 'カテゴリ管理テーブル';

CREATE TABLE task (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'タスクID',
  title VARCHAR(100) NOT NULL COMMENT 'タイトル',
  description TEXT NULL COMMENT '詳細',
  status VARCHAR(20) NOT NULL DEFAULT 'TODO' COMMENT 'ステータス',
  due_date DATE NULL COMMENT '期限',
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '優先度',
  category_id BIGINT NULL COMMENT 'カテゴリID',
  sort_order INT NOT NULL COMMENT '表示順',
  completed_at DATETIME NULL COMMENT '完了日時',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
  CONSTRAINT fk_task_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE SET NULL
) COMMENT = 'タスク管理テーブル';
