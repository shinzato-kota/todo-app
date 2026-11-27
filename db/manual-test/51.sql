-- 手動テストの項目51のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  task (title, status, priority, sort_order)
VALUES
  ('planのタスクA', 'TODO', 'MEDIUM', 1),
  ('メモのタスクB', 'TODO', 'MEDIUM', 2);
