-- 手動テストの項目23のテストデータ
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
  ('APIの設計', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 1),
  ('APIの実装', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 2),
  ('APIの調査', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 3),
  ('APIのテスト', 'IN_PROGRESS', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 4),
  ('画面の設計', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 5);
