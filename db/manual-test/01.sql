-- 手動テストの項目1のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name, color)
VALUES
  ('仕事', '#1e88e5'),
  ('勉強', '#8e24aa');

INSERT INTO
  task (title, status, priority, category_id, sort_order)
VALUES
  ('タスクA', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 1),
  ('タスクB', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 2),
  ('タスクC', 'TODO', 'MEDIUM', NULL, 3);
