-- 手動テストの項目20のテストデータ
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
    '期限切れの進行中',
    '1行目の詳細\n2行目の詳細📝',
    'IN_PROGRESS',
    DATE_SUB(CURRENT_DATE, INTERVAL 2 DAY),
    'HIGH',
    (SELECT id FROM category WHERE name = '仕事'),
    1
  ),
  ('今日が期限の未着手', NULL, 'TODO', CURRENT_DATE, 'MEDIUM', (SELECT id FROM category WHERE name = '勉強'), 2),
  ('7日後の保留', NULL, 'ON_HOLD', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 'LOW', NULL, 3),
  ('期限なしの進行中', NULL, 'IN_PROGRESS', NULL, 'MEDIUM', NULL, 4);
