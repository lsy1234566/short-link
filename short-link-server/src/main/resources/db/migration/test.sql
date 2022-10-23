
-- 乐观锁
INSERT INTO compression_code_num(current_num,end_num,work_id,version_id)
SELECT 916132832 AS current_num,916133832 AS end_num,1 AS  work_id,1 AS version_id
FROM DUAL WHERE (SELECT COUNT(0) FROM compression_code_num WHERE work_id=1 FOR UPDATE)<=0;

COMMIT;



UPDATE compression_code_num SET current_num=2112, end_num=3, version_id=3 WHERE work_id=1 AND version_id=2

-- 锁-初始化
BEGIN;
INSERT INTO compression_code_num(current_num, end_num, work_id, version_id)
SELECT 916132832 AS current_num,
       916133832     AS end_num,
       1     AS work_id,
       1  AS version_id
FROM DUAL
WHERE (SELECT COUNT(0) FROM compression_code_num WHERE work_id = 1 FOR         UPDATE) <= 0;
commit;

-- 唯一型索引解决大小写问题
ALTER TABLE compression_code MODIFY compression_code VARCHAR(16) BINARY;
