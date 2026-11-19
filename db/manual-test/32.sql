-- 手動テストの項目32のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('タスクA', 'TODO', 'MEDIUM', 1),
  ('タスクB', 'TODO', 'MEDIUM', 2),
  ('タスクC', 'TODO', 'MEDIUM', 3),
  ('タスクD', 'TODO', 'MEDIUM', 4);
