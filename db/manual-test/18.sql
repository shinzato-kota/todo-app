-- 手動テストの項目18のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name, color)
VALUES
  ('勉強', '#8e24aa'),
  ('仕事', '#1e88e5');

INSERT INTO
  task (
    title,
    status,
    priority,
    category_id,
    sort_order,
    completed_at
  )
VALUES
  ('仕事の未着手A', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 1, NULL),
  ('仕事の未着手B', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 2, NULL),
  ('仕事の完了A', 'DONE', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 3, CURRENT_TIMESTAMP),
  ('仕事の完了B', 'DONE', 'MEDIUM', (SELECT id FROM category WHERE name = '仕事'), 4, CURRENT_TIMESTAMP),
  ('勉強の未着手', 'TODO', 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 5, NULL),
  ('勉強の完了', 'DONE', 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 6, CURRENT_TIMESTAMP),
  ('カテゴリなしの未着手', 'TODO', 'MEDIUM', NULL, 7, NULL),
  ('カテゴリなしの完了', 'DONE', 'MEDIUM', NULL, 8, CURRENT_TIMESTAMP);
