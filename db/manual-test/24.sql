-- 手動テストの項目24のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('保留A', 'ON_HOLD', 'MEDIUM', 1),
  ('未着手A', 'TODO', 'MEDIUM', 2),
  ('保留B', 'ON_HOLD', 'MEDIUM', 3),
  ('未着手B', 'TODO', 'MEDIUM', 4);
