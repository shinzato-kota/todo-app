-- データベース・taskテーブル作成(DBファースト方式)

CREATE DATABASE IF NOT EXISTS tododb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE tododb;

CREATE TABLE task (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'タスクID',
  title        VARCHAR(100)    NOT NULL COMMENT 'タイトル',
  status       VARCHAR(20)     NOT NULL DEFAULT 'TODO' COMMENT 'ステータス',
  completed_at DATETIME        NULL COMMENT '完了日時',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時'
) COMMENT='タスク管理テーブル';
