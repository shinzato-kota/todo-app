-- 手動テストの項目35のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, description, status, priority, sort_order)
VALUES
  ('タスクA', NULL, 'TODO', 'MEDIUM', 1),
  ('タスクB', 'xyz', 'TODO', 'MEDIUM', 2),
  ('タスクC', 'xyz', 'TODO', 'MEDIUM', 3),
  ('タスクD', 'xyz', 'TODO', 'MEDIUM', 4);
