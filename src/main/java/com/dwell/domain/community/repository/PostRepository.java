package com.dwell.domain.community.repository;

import com.dwell.domain.community.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 내 건물 게시물 ID 목록 (게시판·제목·태그 필터)
    // recommended면 내 케이스 관련 태그가 있는 글 -> 좋아요 많은 글 -> 최신 순, 아니면 최신 순
    // null 파라미터는 PostgreSQL이 타입을 못 정해서 cast, keywords는 빈 목록이면 IN ()이 깨져서 호출 쪽에서 빈 값 하나를 넣음
    @Query(value = "select p.id from post p "
            + "where p.building_id = :buildingId "
            + "and (cast(:board as text) is null or p.board = cast(:board as text)) "
            + "and (cast(:q as text) is null or p.title ilike '%' || cast(:q as text) || '%') "
            + "and (cast(:tag as text) is null or exists (select 1 from post_tag pt join tag t on t.id = pt.tag_id "
            + "     where pt.post_id = p.id and t.name = cast(:tag as text))) "
            + "order by "
            + "case when :recommended and exists (select 1 from post_tag pt join tag t on t.id = pt.tag_id "
            + "     where pt.post_id = p.id and t.name in (:keywords)) then 1 else 0 end desc, "
            + "case when :recommended then (select count(*) from post_like pl where pl.post_id = p.id) else 0 end desc, "
            + "p.created_at desc, p.id desc",
            countQuery = "select count(*) from post p "
                    + "where p.building_id = :buildingId "
                    + "and (cast(:board as text) is null or p.board = cast(:board as text)) "
                    + "and (cast(:q as text) is null or p.title ilike '%' || cast(:q as text) || '%') "
                    + "and (cast(:tag as text) is null or exists (select 1 from post_tag pt join tag t on t.id = pt.tag_id "
                    + "     where pt.post_id = p.id and t.name = cast(:tag as text)))",
            nativeQuery = true)
    Page<Long> findIdsInBuilding(Long buildingId, String board, String q, String tag,
                                 boolean recommended, Collection<String> keywords, Pageable pageable);

    // 검색창 제목 추천 (내 건물, 제목에 포함, 최신 순 5개)
    List<Post> findTop5ByBuildingIdAndTitleContainingIgnoreCaseOrderByCreatedAtDescIdDesc(Long buildingId, String title);

    // 태그 검색 결과 중 내 케이스 관련 태그도 달린 글 수 ("3 related to your case")
    @Query(value = "select count(*) from post p "
            + "where p.building_id = :buildingId "
            + "and exists (select 1 from post_tag pt join tag t on t.id = pt.tag_id where pt.post_id = p.id and t.name = :tag) "
            + "and exists (select 1 from post_tag pt join tag t on t.id = pt.tag_id where pt.post_id = p.id and t.name in (:keywords))",
            nativeQuery = true)
    long countRelatedInBuildingByTag(Long buildingId, String tag, Collection<String> keywords);
}
