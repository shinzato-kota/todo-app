-- 手動テストの項目43のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order, completed_at)
VALUES
  ('タスクA', 'TODO', 'MEDIUM', 1, NULL),
  ('タスクB', 'TODO', 'MEDIUM', 2, NULL),
  ('タスクC', 'IN_PROGRESS', 'MEDIUM', 3, NULL),
  ('タスクD', 'DONE', 'MEDIUM', 4, CURRENT_TIMESTAMP);
