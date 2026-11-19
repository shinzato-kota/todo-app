-- 手動テストの項目7のテストデータ
-- タスクとカテゴリをすべて消してから入れる
USE tododb;

DELETE FROM task;
DELETE FROM category;

INSERT INTO
  category (name, color)
VALUES
  ('仕事', '#1e88e5');

INSERT INTO
  task (
    title,
    description,
    status,
    due_date,
    priority,
    category_id,
    sort_order
  )
VALUES
  (
    '編集前',
    '変えない詳細',
    'IN_PROGRESS',
    DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY),
    'HIGH',
    (SELECT id FROM category WHERE name = '仕事'),
    1
  ),
  ('別のタスク', NULL, 'TODO', NULL, 'MEDIUM', NULL, 2);
