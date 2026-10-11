package com.dwell.domain.notice.repository;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 발송 완료된 통지만 (배너의 Notice sent / Response due)
    List<Notice> findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(Case caseEntity);
}
