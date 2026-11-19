-- 手動テストの項目40のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name)
VALUES
  ('仕事');

INSERT INTO
  task (title, status, priority, category_id, sort_order)
VALUES
  (REPEAT('⌨️', 50), 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 1),
  (REPEAT('a', 100), 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 2),
  (REPEAT('あ', 100), 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 3);
