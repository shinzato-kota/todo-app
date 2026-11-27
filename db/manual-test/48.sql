-- 手動テストの項目48のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('進行中のタスクA', 'IN_PROGRESS', 'MEDIUM', 1),
  ('進行中のタスクB', 'IN_PROGRESS', 'MEDIUM', 2),
  ('未着手のタスクC', 'TODO', 'MEDIUM', 3),
  ('未着手のタスクD', 'TODO', 'MEDIUM', 4);
