-- v2 매장은 v1 매장과 같은 ID를 쓴다(V29 복사). v1 RAG 색인이 돌려주는 매장 ID를 그대로 v2 매장으로 읽으므로,
-- 관리자가 앱에서 새로 만든 매장이 이후 v1에 생길 매장 ID와 겹치지 않게 번호를 멀리 띄운다.
ALTER TABLE tb_v2_shop AUTO_INCREMENT = 10000000;
