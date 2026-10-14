package com.dwell.domain.home.service;

import com.dwell.domain.building.dto.response.AttentionResponse;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.building.service.PlaceService;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseChecklistItem;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.AffectedArea;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.enums.IssueDetail;
import com.dwell.domain.cases.repository.CaseChecklistItemRepository;
import com.dwell.domain.cases.repository.CaseEventRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.cases.service.CaseBannerFactory;
import com.dwell.domain.cases.service.CaseTimelineStepFactory;
import com.dwell.domain.home.dto.response.HomeCaseResponse;
import com.dwell.domain.home.dto.response.HomeChecklistItemResponse;
import com.dwell.domain.home.dto.response.HomeResponse;
import com.dwell.domain.notice.entity.Notice;
import com.dwell.domain.notice.repository.NoticeRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.exception.UserErrorCode;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.Location;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class HomeService {

    private final UserRepository userRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final CaseRepository caseRepository;
    private final CaseEventRepository caseEventRepository;
    private final NoticeRepository noticeRepository;
    private final CaseChecklistItemRepository caseChecklistItemRepository;
    private final CaseTimelineStepFactory caseTimelineStepFactory;
    private final CaseBannerFactory caseBannerFactory;
    private final PlaceService placeService;

    // 홈 화면 조회
    public HomeResponse getHome(Long userId) {

        // DB에서 사용자 조회
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("[HomeService] Home lookup failed: user not found: userId={}", userId);
            return new CustomException(UserErrorCode.USER_NOT_FOUND);
        });

        // 진행 중인 케이스 조회 (Resolved·삭제된 케이스 제외, 이전 주소 케이스도 포함)
        List<Case> cases = caseRepository.findAllByUserIdAndDeletedAtIsNullAndStageNot(userId, CaseStage.RESOLVED);

        // 현재 시각
        LocalDateTime now = LocalDateTime.now();

        // 케이스 카드 생성 (응답 기한은 정렬할 때만 사용)
        List<HomeCaseResponse> caseCards = new ArrayList<>();
        Map<Long, LocalDateTime> dueAts = new HashMap<>();
        for (Case caseEntity : cases) {

            // 타임라인 이벤트, 발송된 통지 조회 (진행 스텝·응답 기한 계산용)
            List<CaseEvent> events = caseEventRepository.findAllByCaseEntityOrderByOccurredAtAscIdAsc(caseEntity);
            List<Notice> sentNotices = noticeRepository.findAllByCaseEntityAndSentAtIsNotNullOrderBySentAtAsc(caseEntity);

            // 응답 기한 계산 (기한이 없는 단계면 null)
            dueAts.put(caseEntity.getId(), caseBannerFactory.create(caseEntity, events, sentNotices, now).getDueAt());

            // 케이스 카드 객체 생성
            caseCards.add(HomeCaseResponse.builder()
                    .caseId(caseEntity.getId())
                    .caseNumber(caseEntity.getCaseNumber())
                    .title(toIssueLabel(caseEntity))
                    .locationLabel(toLocationLabel(caseEntity))
                    .stage(caseEntity.getStage())
                    .updatedAt(caseEntity.getModifiedAt())
                    .timelineSteps(caseTimelineStepFactory.create(caseEntity, events))
                    .checklist(toChecklist(caseEntity))
                    .build());
        }

        // 케이스 카드 정렬 (응답 기한이 가까운 순, 기한이 없으면 최근 업데이트 순)
        caseCards.sort(Comparator
                .comparing((HomeCaseResponse card) -> dueAts.get(card.getCaseId()), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(HomeCaseResponse::getUpdatedAt, Comparator.reverseOrder()));

        // 내 건물 Needs Your Attention 조회 (집 정보가 없으면 null)
        AttentionResponse attention = null;
        UserPlace userPlace = userPlaceRepository.findByUserId(userId).orElse(null);
        if (userPlace != null) {
            attention = placeService.getAttention(userPlace.getBuilding().getId());
        }

        // 로그 출력
        log.info("[HomeService] Home retrieved: userId={}, cases={}, attention={}", userId, caseCards.size(), attention != null);

        // 응답 세팅
        return HomeResponse.builder()
                .user(HomeResponse.UserInfo.builder()
                        .nickname(user.getNickname())
                        .build())
                .cases(caseCards)
                .attention(attention)
                .build();
    }

    // ========== 헬퍼 함수 ==========

    // 카드 제목 (세부 이슈 이름, Other면 직접 입력한 값)
    private String toIssueLabel(Case caseEntity) {
        if (caseEntity.getIssueDetail().equals(IssueDetail.OTHER)) {
            return caseEntity.getIssueDetailOther();
        }
        return IssueDetail.valueOf(caseEntity.getIssueDetail()).getLabel();
    }

    // 카드 위치 (공간 - 부위, Entire Unit이면 공간만)
    private String toLocationLabel(Case caseEntity) {

        // 공간 (Other면 직접 입력한 값)
        String place = caseEntity.getLocation().getLabel();
        if (caseEntity.getLocation() == Location.OTHER) {
            place = caseEntity.getLocationOther();
        }

        // 부위가 없으면 공간만
        if (caseEntity.getAffectedArea() == null) {
            return place;
        }

        // 부위 (Other면 직접 입력한 값)
        String area = caseEntity.getAffectedArea().getLabel();
        if (caseEntity.getAffectedArea() == AffectedArea.OTHER) {
            area = caseEntity.getAffectedAreaOther();
        }

        return place + " - " + area;
    }

    // 체크리스트 조회 (케이스 상세와 같은 항목)
    private List<HomeChecklistItemResponse> toChecklist(Case caseEntity) {
        List<CaseChecklistItem> items = caseChecklistItemRepository.findAllByCaseEntityOrderBySortOrderAscIdAsc(caseEntity);

        List<HomeChecklistItemResponse> responses = new ArrayList<>();
        for (CaseChecklistItem item : items) {
            responses.add(HomeChecklistItemResponse.builder()
                    .itemId(item.getId())
                    .text(new LocalizedText(item.getText(), item.getTextNative()))
                    .checked(item.isChecked())
                    .build());
        }
        return responses;
    }
}
