-- V29가 v1 매장 사진 주소를 그대로 복사했다. v1은 응답할 때 저장소 주소를 CDN 주소로 바꿔 줬지만 v2는 그대로 내보내서,
-- 공개되지 않은 S3 호환 주소(…compat.objectstorage…)는 앱에서 404였다. ramenshop 버킷 객체는 images.raota.net/<객체 키>로 열린다.
UPDATE tb_v2_shop_image
SET url = CONCAT('https://images.raota.net/', SUBSTRING(url, LOCATE('/b/ramenshop/o/', url) + CHAR_LENGTH('/b/ramenshop/o/')))
WHERE url LIKE 'https://%objectstorage.ap-chuncheon-1.oraclecloud.com/n/%/b/ramenshop/o/%';
