-- 手動テストの項目33のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('タスクA', 'IN_PROGRESS', 'MEDIUM', 1),
  ('タスクB', 'TODO', 'MEDIUM', 2),
  ('タスクC', 'IN_PROGRESS', 'MEDIUM', 3),
  ('タスクD', 'IN_PROGRESS', 'MEDIUM', 4);
