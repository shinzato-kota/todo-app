-- 手動テストの項目44のテストデータ
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
  ('仕事のplanA', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 1),
  ('仕事のplanB', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 2),
  ('仕事のメモA', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 3),
  ('仕事のメモB', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 4),
  ('勉強のplanA', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 5),
  ('勉強のplanB', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 6);
