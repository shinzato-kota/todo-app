-- 手動テストの項目50のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name, color)
VALUES
  ('仕事', '#1e88e5');

INSERT INTO
  task (title, status, due_date, priority, category_id, sort_order)
VALUES
  (
    '仕事のplan',
    'IN_PROGRESS',
    DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY),
    'HIGH',
    (SELECT id FROM category WHERE name = '仕事'),
    1
  ),
  ('仕事のメモ', 'TODO', NULL, 'LOW', (SELECT id FROM category WHERE name = '仕事'), 2),
  ('カテゴリなしのメモ', 'TODO', NULL, 'MEDIUM', NULL, 3);
