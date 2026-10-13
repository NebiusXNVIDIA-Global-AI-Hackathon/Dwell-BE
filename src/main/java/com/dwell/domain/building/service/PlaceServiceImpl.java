package com.dwell.domain.building.service;

import com.dwell.domain.building.dto.request.InspectionCreateRequest;
import com.dwell.domain.building.dto.response.BuildingSignalsResponse;
import com.dwell.domain.building.dto.response.CategoryCountResponse;
import com.dwell.domain.building.dto.response.FollowUpTimerResponse;
import com.dwell.domain.building.dto.response.HpdHistoryResponse;
import com.dwell.domain.building.dto.response.HpdRecordResponse;
import com.dwell.domain.building.dto.response.InspectionCreateResponse;
import com.dwell.domain.building.dto.response.InspectionGuideResponse;
import com.dwell.domain.building.dto.response.InspectionPrefillResponse;
import com.dwell.domain.building.dto.response.LandlordInsightResponse;
import com.dwell.domain.building.dto.response.MonthCountResponse;
import com.dwell.domain.building.dto.response.OpenClosedCountResponse;
import com.dwell.domain.building.dto.response.PlaceSummaryResponse;
import com.dwell.domain.building.dto.response.PrimarySignalResponse;
import com.dwell.domain.building.dto.response.QuarterCountResponse;
import com.dwell.domain.building.dto.response.RecordsInsightResponse;
import com.dwell.domain.building.dto.response.SignalDetailResponse;
import com.dwell.domain.building.dto.response.SignalItemResponse;
import com.dwell.domain.building.dto.response.SymptomResponse;
import com.dwell.domain.building.dto.response.TrackRecordResponse;
import com.dwell.domain.building.dto.response.ViolationClassCountResponse;
import com.dwell.domain.building.entity.Building;
import com.dwell.domain.building.entity.BuildingSignal;
import com.dwell.domain.building.entity.HpdViolation;
import com.dwell.domain.building.entity.InspectionResult;
import com.dwell.domain.building.entity.UserPlace;
import com.dwell.domain.building.enums.FollowUpReason;
import com.dwell.domain.building.enums.HpdHistoryTab;
import com.dwell.domain.building.enums.HpdRecordType;
import com.dwell.domain.building.enums.HpdStatus;
import com.dwell.domain.building.enums.InsightStatus;
import com.dwell.domain.building.enums.InspectionOutcome;
import com.dwell.domain.building.enums.LandlordRating;
import com.dwell.domain.building.exception.BuildingErrorCode;
import com.dwell.domain.building.repository.BuildingSignalRepository;
import com.dwell.domain.building.repository.HpdViolationRepository;
import com.dwell.domain.building.repository.InspectionResultRepository;
import com.dwell.domain.building.repository.UserPlaceRepository;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.community.service.CommunityService;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlaceServiceImpl implements PlaceService {

    private static final int HPD_PERIOD_YEARS = 3;
    private static final int MIN_FIXED_VIOLATIONS = 3; // 집주인 배지를 내는 최소 closed 위반 수
    private static final int MIN_RECORDS = 3; // Records 탭 추세를 보여주는 최소 기록 수
    private static final int REPEATED_MIN_COUNT = 3; // 최근 1년 같은 민원 3회 이상이면 반복 민원
    private static final int NYC_AVG_DAYS_TO_FIX = 30; // NYC 평균 위반 수리일 (와프 기준값, 아직 HPD 전체 데이터로 계산하지 않음)
    private static final int GAUGE_MAX_DAYS = 90;
    private static final int DEFAULT_FOLLOW_UP_HOURS = 72;
    private static final int SHORTENED_FOLLOW_UP_HOURS = 48;
    private static final int MONTHS_IN_TREND = 36;
    private static final int QUARTERS_IN_CHART = 12;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String HPD_ONLINE_URL = "https://hpdonline.nyc.gov/hpdonline/";

    // 이슈 카테고리 -> [이름, Rising Issue 설명]
    private static final Map<IssueCategory, List<String>> ISSUE_LABELS = Map.of(
            IssueCategory.HEAT_HOT_WATER, List.of("Heating issues", "A few neighbors reported no heat. Check your unit too."),
            IssueCategory.WATER_FLOODING, List.of("Water leak", "A few neighbors reported water leaks. Check your ceilings and walls."),
            IssueCategory.PLUMBING, List.of("Plumbing issues", "A few neighbors reported plumbing problems."),
            IssueCategory.MOLD_MOISTURE, List.of("Mold", "A few neighbors reported mold or moisture."),
            IssueCategory.PESTS, List.of("Pests", "A few neighbors reported pest activity."),
            IssueCategory.ELECTRICITY, List.of("Electrical issues", "A few neighbors reported power or outlet problems."),
            IssueCategory.GAS, List.of("Gas issues", "A few neighbors reported gas problems."),
            IssueCategory.APPLIANCES, List.of("Appliance issues", "A few neighbors reported broken appliances."),
            IssueCategory.VENTILATION, List.of("Ventilation issues", "A few neighbors reported ventilation problems."),
            IssueCategory.SAFETY, List.of("Safety issues", "A few neighbors reported safety concerns."));

    private final UserPlaceRepository userPlaceRepository;
    private final BuildingSignalRepository buildingSignalRepository;
    private final HpdViolationRepository hpdViolationRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final CaseRepository caseRepository;
    private final CommunityService communityService;

    // 내 건물 요약 서비스
    @Override
    public PlaceSummaryResponse getSummary(Long userId) {

        log.info("[PlaceService] 내 건물 요약 서비스 - 시작: userId={}", userId);

        // 내 집 조회
        UserPlace place = findPlace(userId);
        Building building = place.getBuilding();

        // 시그널 (메인 패턴은 Building Signals 화면에만, 메인 화면은 나머지 Rising Issue)
        List<BuildingSignal> signals = buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(building.getId());
        BuildingSignal primary = findPrimary(signals, activeCategories(userId));

        log.info("[PlaceService] 내 건물 요약 서비스 - 완료: userId={}, signals={}", userId, signals.size());

        return PlaceSummaryResponse.builder()
                .address(toAddress(place))
                .bbl(building.getBbl())
                .risingIssues(toRisingIssues(signals, primary))
                .build();
    }

    // Building Signals 목록 서비스
    @Override
    public BuildingSignalsResponse getSignals(Long userId) {

        log.info("[PlaceService] Building Signals 서비스 - 시작: userId={}", userId);

        // 내 집 조회
        UserPlace place = findPlace(userId);

        // 시그널 + 내 진행 중 케이스와 같은 이슈 중 가장 강한 패턴
        List<BuildingSignal> signals = buildingSignalRepository.findAllByBuildingIdOrderByReportCountDescComputedAtDescIdDesc(place.getBuilding().getId());
        BuildingSignal primary = findPrimary(signals, activeCategories(userId));

        log.info("[PlaceService] Building Signals 서비스 - 완료: userId={}, signals={}, primary={}",
                userId, signals.size(), primary == null ? null : primary.getId());

        return BuildingSignalsResponse.builder()
                .primary(primary == null ? null : PrimarySignalResponse.builder()
                        .signalId(primary.getId())
                        .title(labelOf(primary.getIssueCategory()) + " pattern detected")
                        .issueCategory(primary.getIssueCategory())
                        .issueLabel(labelOf(primary.getIssueCategory()))
                        .reportCount(primary.getReportCount())
                        .reportedIn(toReportedIn(primary.getBreakdown()))
                        .windowLabel(toWindowLabel(primary.getWindowDays()))
                        .relatedToMyCase(true)
                        .build())
                .risingIssues(toRisingIssues(signals, primary))
                .build();
    }

    // Issue Details 서비스
    @Override
    public SignalDetailResponse getSignal(Long userId, Long signalId) {

        log.info("[PlaceService] Issue Details 서비스 - 시작: userId={}, signalId={}", userId, signalId);

        // 내 건물 시그널 조회
        UserPlace place = findPlace(userId);
        BuildingSignal signal = findSignalInMyBuilding(place, signalId);

        return SignalDetailResponse.builder()
                .signalId(signal.getId())
                .title(labelOf(signal.getIssueCategory()))
                .issueCategory(signal.getIssueCategory())
                .relatedToMyCase(activeCategories(userId).contains(signal.getIssueCategory()))
                .insight(signal.getInsight())
                .posts(communityService.getIssuePosts(place.getBuilding().getId(), signal.getIssueCategory()))
                .build();
    }

    // Check My Unit 체크리스트 서비스
    @Override
    public InspectionGuideResponse getInspectionGuide(Long userId, Long signalId) {

        log.info("[PlaceService] Check My Unit 체크리스트 서비스 - 시작: userId={}, signalId={}", userId, signalId);

        // 내 건물 시그널 조회
        BuildingSignal signal = findSignalInMyBuilding(findPlace(userId), signalId);

        // 체크리스트 (시그널 배치가 저장한 guided_inspection, 없으면 빈 목록)
        List<SymptomResponse> symptoms = toSymptoms(signal.getGuidedInspection());

        log.info("[PlaceService] Check My Unit 체크리스트 서비스 - 완료: signalId={}, symptoms={}", signalId, symptoms.size());

        return InspectionGuideResponse.builder()
                .signalId(signal.getId())
                .issueCategory(signal.getIssueCategory())
                .subtitle(labelOf(signal.getIssueCategory()) + " · based on " + signal.getReportCount()
                        + (signal.getReportCount() == 1 ? " neighbor report" : " neighbor reports"))
                .symptoms(symptoms)
                .build();
    }

    // Check My Unit 결과 저장 서비스
    @Override
    @Transactional
    public InspectionCreateResponse createInspection(Long userId, InspectionCreateRequest request) {

        log.info("[PlaceService] Check My Unit 결과 저장 서비스 - 시작: userId={}, signalId={}", userId, request.getSignalId());

        // 내 집 + 내 건물 시그널 조회
        UserPlace place = findPlace(userId);
        BuildingSignal signal = findSignalInMyBuilding(place, request.getSignalId());

        // 체크리스트에 있는 증상인지 확인 (중복 제거)
        List<String> symptoms = request.getSymptoms().stream().distinct().toList();
        Set<String> allowed = toSymptoms(signal.getGuidedInspection()).stream()
                .map(SymptomResponse::getKey)
                .collect(Collectors.toSet());
        if (!allowed.containsAll(symptoms)) {
            log.warn("[PlaceService] Check My Unit 결과 저장 서비스 - 체크리스트에 없는 증상: signalId={}, symptoms={}", signal.getId(), symptoms);
            throw new CustomException(BuildingErrorCode.INVALID_SYMPTOM);
        }

        // 결과 저장 (체크 1개 이상이라 항상 FOUND_ISSUE, Create Case로 케이스가 생기면 case_id가 연결됨)
        InspectionResult result = inspectionResultRepository.save(InspectionResult.builder()
                .user(place.getUser())
                .building(place.getBuilding())
                .signal(signal)
                .issueCategory(signal.getIssueCategory())
                .outcome(InspectionOutcome.FOUND_ISSUE)
                .symptoms(symptoms)
                .build());

        log.info("[PlaceService] Check My Unit 결과 저장 서비스 - 완료: inspectionId={}, symptoms={}", result.getId(), symptoms.size());

        return InspectionCreateResponse.builder()
                .inspectionId(result.getId())
                .matchedSigns(symptoms.size())
                .prefill(InspectionPrefillResponse.builder()
                        .issueCategory(signal.getIssueCategory())
                        .patternText(signal.getReportCount() + (signal.getReportCount() == 1 ? " report" : " reports")
                                + " in your building")
                        .build())
                .build();
    }

    // Building Insights · Landlord 탭 서비스
    @Override
    public LandlordInsightResponse getLandlordInsight(Long userId) {

        log.info("[PlaceService] Landlord 탭 서비스 - 시작: userId={}", userId);

        // 최근 3년 HPD 위반
        List<HpdViolation> violations = findRecentRecords(findPlace(userId)).stream()
                .filter(isType(HpdRecordType.VIOLATION))
                .toList();

        // closed 위반의 평균 수리일 (발행일 -> 해결일)
        List<Long> daysToFix = violations.stream()
                .filter(v -> v.getStatus() == HpdStatus.CLOSED && v.getClosedAt() != null)
                .map(v -> ChronoUnit.DAYS.between(v.getReportedAt(), v.getClosedAt()))
                .toList();
        Integer avgDaysToFix = daysToFix.size() < MIN_FIXED_VIOLATIONS ? null
                : (int) Math.round(daysToFix.stream().mapToLong(Long::longValue).average().orElse(0));
        LandlordRating rating = toRating(avgDaysToFix);

        // Slow to fix면 Follow-up timer 단축
        FollowUpTimerResponse followUpTimer = FollowUpTimerResponse.builder()
                .defaultHours(DEFAULT_FOLLOW_UP_HOURS)
                .appliedHours(rating == LandlordRating.SLOW_TO_FIX ? SHORTENED_FOLLOW_UP_HOURS : DEFAULT_FOLLOW_UP_HOURS)
                .reason(rating == LandlordRating.SLOW_TO_FIX ? FollowUpReason.SHORTENED
                        : rating == LandlordRating.NOT_ENOUGH_DATA ? FollowUpReason.NOT_ENOUGH_HISTORY : FollowUpReason.STANDARD)
                .build();

        log.info("[PlaceService] Landlord 탭 서비스 - 완료: userId={}, rating={}, fixed={}", userId, rating, daysToFix.size());

        return LandlordInsightResponse.builder()
                .trackRecord(TrackRecordResponse.builder()
                        .rating(rating)
                        .avgDaysToFix(avgDaysToFix)
                        .nycAvgDaysToFix(NYC_AVG_DAYS_TO_FIX)
                        .gaugeMaxDays(GAUGE_MAX_DAYS)
                        .fixedViolations(daysToFix.size())
                        .minimumRequired(MIN_FIXED_VIOLATIONS)
                        .violations(toCount(violations))
                        .classC(toCount(violations.stream().filter(v -> "C".equals(v.getViolationClass())).toList()))
                        .source(avgDaysToFix != null ? "NYC HPD violations, " + HPD_PERIOD_YEARS + " yrs · computed"
                                : "NYC HPD · " + daysToFix.size() + (daysToFix.size() == 1 ? " closed violation" : " closed violations")
                                + " (" + MIN_FIXED_VIOLATIONS + "+ needed)")
                        .build())
                .followUpTimer(followUpTimer)
                .build();
    }

    // Building Insights · Records 탭 서비스
    @Override
    public RecordsInsightResponse getRecordsInsight(Long userId) {

        log.info("[PlaceService] Records 탭 서비스 - 시작: userId={}", userId);

        // 최근 3년 HPD 민원·위반
        List<HpdViolation> records = findRecentRecords(findPlace(userId));
        List<HpdViolation> complaints = records.stream().filter(isType(HpdRecordType.COMPLAINT)).toList();
        List<HpdViolation> violations = records.stream().filter(isType(HpdRecordType.VIOLATION)).toList();
        boolean enough = records.size() >= MIN_RECORDS;

        // 반복 민원 (최근 1년 같은 카테고리 3회 이상 중 가장 많은 것)
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        CategoryCountResponse repeatedIssue = !enough ? null : countByCategory(complaints.stream()
                .filter(c -> !c.getReportedAt().isBefore(oneYearAgo))
                .toList()).stream()
                .filter(c -> c.getCount() >= REPEATED_MIN_COUNT)
                .findFirst()
                .orElse(null);

        log.info("[PlaceService] Records 탭 서비스 - 완료: userId={}, complaints={}, violations={}", userId, complaints.size(), violations.size());

        return RecordsInsightResponse.builder()
                .status(enough ? InsightStatus.OK : InsightStatus.NOT_ENOUGH_DATA)
                .periodYears(HPD_PERIOD_YEARS)
                .complaints(toCount(complaints))
                .violations(toCount(violations))
                .minimumRequired(MIN_RECORDS)
                .complaintsByCategory(enough ? countByCategory(complaints) : null)
                .repeatedIssue(repeatedIssue)
                .complaintsPerMonth(enough ? countPerMonth(complaints) : null)
                .source("NYC HPD · updated daily")
                .build();
    }

    // HPD History 서비스
    @Override
    public HpdHistoryResponse getHpdHistory(Long userId, HpdHistoryTab tab, int page, int size) {

        log.info("[PlaceService] HPD History 서비스 - 시작: userId={}, tab={}, page={}, size={}", userId, tab, page, size);

        UserPlace place = findPlace(userId);
        HpdHistoryResponse.HpdHistoryResponseBuilder response = HpdHistoryResponse.builder()
                .source("NYC HPD public records")
                .periodYears(HPD_PERIOD_YEARS)
                .hpdOnlineUrl(HPD_ONLINE_URL);

        // Complaints / Violations 탭: 기록 목록 (최신 순)
        if (tab != HpdHistoryTab.OVERVIEW) {
            HpdRecordType type = tab == HpdHistoryTab.COMPLAINTS ? HpdRecordType.COMPLAINT : HpdRecordType.VIOLATION;
            Page<HpdViolation> records = hpdViolationRepository
                    .findByBuildingIdAndRecordTypeAndReportedAtGreaterThanEqualOrderByReportedAtDescIdDesc(
                            place.getBuilding().getId(), type, periodStart(),
                            PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));

            log.info("[PlaceService] HPD History 서비스 - 완료: userId={}, tab={}, total={}", userId, tab, records.getTotalElements());

            return response
                    .content(records.getContent().stream()
                            .map(r -> HpdRecordResponse.builder()
                                    .date(r.getReportedAt())
                                    .kind(r.getRecordType())
                                    .violationClass(r.getViolationClass())
                                    .category(r.getCategory())
                                    .status(r.getStatus())
                                    .build())
                            .toList())
                    .page(records.getNumber())
                    .size(records.getSize())
                    .totalElements(records.getTotalElements())
                    .hasNext(records.hasNext())
                    .build();
        }

        // Overview 탭: 요약 + 클래스별 위반 + 분기별 차트
        List<HpdViolation> records = findRecentRecords(place);
        List<HpdViolation> complaints = records.stream().filter(isType(HpdRecordType.COMPLAINT)).toList();
        List<HpdViolation> violations = records.stream().filter(isType(HpdRecordType.VIOLATION)).toList();

        List<ViolationClassCountResponse> byClass = List.of("A", "B", "C").stream()
                .map(violationClass -> {
                    List<HpdViolation> inClass = violations.stream().filter(v -> violationClass.equals(v.getViolationClass())).toList();
                    return ViolationClassCountResponse.builder()
                            .violationClass(violationClass)
                            .total(inClass.size())
                            .open(inClass.stream().filter(v -> v.getStatus() == HpdStatus.OPEN).count())
                            .build();
                })
                .toList();

        log.info("[PlaceService] HPD History 서비스 - 완료: userId={}, tab=OVERVIEW, records={}", userId, records.size());

        return response
                .complaints(toCount(complaints))
                .violations(toCount(violations))
                .violationsByClass(byClass)
                .perQuarter(countPerQuarter(complaints, violations))
                .build();
    }

    // ========== 헬퍼 함수 ==========

    // 내 집 조회 (없으면 400)
    private UserPlace findPlace(Long userId) {
        return userPlaceRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[PlaceService] 등록된 집이 없습니다: userId={}", userId);
                    return new CustomException(BuildingErrorCode.PLACE_NOT_REGISTERED);
                });
    }

    // 내 건물 시그널 조회 (없거나 다른 건물이면 404)
    private BuildingSignal findSignalInMyBuilding(UserPlace place, Long signalId) {
        return buildingSignalRepository.findById(signalId)
                .filter(signal -> signal.getBuilding().getId().equals(place.getBuilding().getId()))
                .orElseThrow(() -> {
                    log.warn("[PlaceService] 시그널 조회 실패 - 없거나 다른 건물 시그널: signalId={}", signalId);
                    return new CustomException(BuildingErrorCode.SIGNAL_NOT_FOUND);
                });
    }

    // 내 진행 중(Resolved 아님) 케이스 이슈 카테고리
    private Set<IssueCategory> activeCategories(Long userId) {
        return new HashSet<>(caseRepository.findActiveIssueCategoriesByUserId(userId));
    }

    // 메인 패턴: 내 케이스와 같은 이슈 중 신고가 가장 많은 시그널 (시그널은 이미 신고 많은 순)
    private BuildingSignal findPrimary(List<BuildingSignal> signals, Set<IssueCategory> myCategories) {
        return signals.stream()
                .filter(signal -> myCategories.contains(signal.getIssueCategory()))
                .findFirst()
                .orElse(null);
    }

    // 메인 패턴을 뺀 나머지 시그널 -> Rising Issue
    private List<SignalItemResponse> toRisingIssues(List<BuildingSignal> signals, BuildingSignal primary) {
        return signals.stream()
                .filter(signal -> signal != primary)
                .map(signal -> SignalItemResponse.builder()
                        .signalId(signal.getId())
                        .issueCategory(signal.getIssueCategory())
                        .title(labelOf(signal.getIssueCategory()))
                        .description(ISSUE_LABELS.get(signal.getIssueCategory()).get(1))
                        .reportCount(signal.getReportCount())
                        .build())
                .toList();
    }

    private String labelOf(IssueCategory category) {
        return ISSUE_LABELS.get(category).get(0);
    }

    // 주소 (245 W 107th St, Apt 4B, New York, NY 10025)
    private String toAddress(UserPlace place) {
        Building building = place.getBuilding();
        String unit = place.getUnit() == null || place.getUnit().isBlank() ? "" : ", " + place.getUnit();
        return building.getStreet() + unit + ", " + building.getCity() + ", " + building.getState() + " " + building.getZip();
    }

    // 신고 위치 (시그널 breakdown.by_location: {"Bathroom": 3, ...} 많은 순, 또는 ["Bathroom", ...])
    private List<String> toReportedIn(Map<String, Object> breakdown) {
        Object byLocation = breakdown == null ? null : breakdown.get("by_location");
        if (byLocation instanceof Map<?, ?> counts) {
            return counts.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<?, ?> e) ->
                            e.getValue() instanceof Number n ? n.doubleValue() : 0).reversed())
                    .map(e -> String.valueOf(e.getKey()))
                    .toList();
        }
        if (byLocation instanceof List<?> names) {
            return names.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    // 집계 기간 문구 (60일 -> 2 months, 30일 -> 30 days)
    private String toWindowLabel(int windowDays) {
        if (windowDays >= 60) {
            return Math.round(windowDays / 30.0) + " months";
        }
        return windowDays + (windowDays == 1 ? " day" : " days");
    }

    // guided_inspection [{key, label: {en, native} 또는 문자열}] -> 체크리스트
    private List<SymptomResponse> toSymptoms(List<Map<String, Object>> guidedInspection) {
        if (guidedInspection == null) {
            return List.of();
        }
        return guidedInspection.stream()
                .filter(item -> item.get("key") != null)
                .map(item -> SymptomResponse.builder()
                        .key(String.valueOf(item.get("key")))
                        .label(toLocalizedText(item.get("label")))
                        .build())
                .toList();
    }

    private LocalizedText toLocalizedText(Object label) {
        if (label instanceof Map<?, ?> map) {
            Object en = map.get("en");
            Object nativeText = map.get("native");
            return new LocalizedText(en == null ? null : String.valueOf(en), nativeText == null ? null : String.valueOf(nativeText));
        }
        return label == null ? null : new LocalizedText(String.valueOf(label), null);
    }

    // 평균 수리일 -> 배지 (NYC 평균 대비 0.8배 이하 Fixes quickly, 1.2배 이하 About average, 그보다 길면 Slow to fix)
    private LandlordRating toRating(Integer avgDaysToFix) {
        if (avgDaysToFix == null) {
            return LandlordRating.NOT_ENOUGH_DATA;
        }
        if (avgDaysToFix <= NYC_AVG_DAYS_TO_FIX * 0.8) {
            return LandlordRating.FIXES_QUICKLY;
        }
        return avgDaysToFix <= NYC_AVG_DAYS_TO_FIX * 1.2 ? LandlordRating.ABOUT_AVERAGE : LandlordRating.SLOW_TO_FIX;
    }

    private LocalDate periodStart() {
        return LocalDate.now().minusYears(HPD_PERIOD_YEARS);
    }

    // 최근 3년 HPD 민원·위반
    private List<HpdViolation> findRecentRecords(UserPlace place) {
        return hpdViolationRepository.findAllByBuildingIdAndReportedAtGreaterThanEqual(place.getBuilding().getId(), periodStart());
    }

    private Predicate<HpdViolation> isType(HpdRecordType type) {
        return record -> record.getRecordType() == type;
    }

    private OpenClosedCountResponse toCount(List<HpdViolation> records) {
        long open = records.stream().filter(r -> r.getStatus() == HpdStatus.OPEN).count();
        return OpenClosedCountResponse.builder()
                .total(records.size())
                .open(open)
                .closed(records.size() - open)
                .build();
    }

    // 카테고리별 건수 (많은 순 -> 이름 순)
    private List<CategoryCountResponse> countByCategory(List<HpdViolation> records) {
        return records.stream()
                .collect(Collectors.groupingBy(HpdViolation::getCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> CategoryCountResponse.builder().category(e.getKey()).count(e.getValue()).build())
                .toList();
    }

    // 월별 민원 (최근 36개월, 오래된 순, 0건인 달 포함)
    private List<MonthCountResponse> countPerMonth(List<HpdViolation> complaints) {
        Map<YearMonth, Long> counts = complaints.stream()
                .collect(Collectors.groupingBy(c -> YearMonth.from(c.getReportedAt()), Collectors.counting()));
        YearMonth current = YearMonth.now();
        List<MonthCountResponse> months = new ArrayList<>();
        for (int i = MONTHS_IN_TREND - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            months.add(MonthCountResponse.builder().month(month.toString()).count(counts.getOrDefault(month, 0L)).build());
        }
        return months;
    }

    // 분기별 민원·위반 (최근 12분기, 오래된 순, 0건인 분기 포함)
    private List<QuarterCountResponse> countPerQuarter(List<HpdViolation> complaints, List<HpdViolation> violations) {
        Map<String, Long> complaintCounts = complaints.stream().collect(Collectors.groupingBy(r -> toQuarter(r.getReportedAt()), Collectors.counting()));
        Map<String, Long> violationCounts = violations.stream().collect(Collectors.groupingBy(r -> toQuarter(r.getReportedAt()), Collectors.counting()));
        Map<String, QuarterCountResponse> quarters = new LinkedHashMap<>();
        LocalDate current = LocalDate.now();
        for (int i = QUARTERS_IN_CHART - 1; i >= 0; i--) {
            String quarter = toQuarter(current.minusMonths(3L * i));
            quarters.put(quarter, QuarterCountResponse.builder()
                    .quarter(quarter)
                    .complaints(complaintCounts.getOrDefault(quarter, 0L))
                    .violations(violationCounts.getOrDefault(quarter, 0L))
                    .build());
        }
        return List.copyOf(quarters.values());
    }

    // 날짜 -> 분기 (2026-08-03 -> 2026-Q3)
    private String toQuarter(LocalDate date) {
        return date.getYear() + "-Q" + date.get(IsoFields.QUARTER_OF_YEAR);
    }
}
