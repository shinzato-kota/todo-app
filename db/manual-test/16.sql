-- 手動テストの項目16のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('進行中で高A', 'IN_PROGRESS', 'HIGH', 1),
  ('進行中で高B', 'IN_PROGRESS', 'HIGH', 2),
  ('進行中で低A', 'IN_PROGRESS', 'LOW', 3),
  ('進行中で低B', 'IN_PROGRESS', 'LOW', 4),
  ('未着手で高A', 'TODO', 'HIGH', 5),
  ('未着手で高B', 'TODO', 'HIGH', 6);
