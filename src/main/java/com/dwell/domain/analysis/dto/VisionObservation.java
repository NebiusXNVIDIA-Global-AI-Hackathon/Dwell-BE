package com.dwell.domain.analysis.dto;

import com.dwell.domain.analysis.enums.AssessedSeverity;

import java.util.List;

/*
튜닝한 Gemini의 사진·영상 1개 판정 (학습 때 형식 그대로) + 서버가 붙인 메타(evidenceId, fileId)
- defectType: water_stain_or_leak | mold | both_leak_and_mold | crack | none | other_defect
 */
public record VisionObservation(Long evidenceId, Long fileId, String defectType, AssessedSeverity severity,
                                String severityReason, String location, List<String> visibleSigns,
                                String activeWaterOrDampness, String extent) {

    // 판정할 수 없을 때 (모델 미설정, 응답 형식 오류, 파일이 너무 큼 등)
    public static VisionObservation inconclusive(Long evidenceId, Long fileId, String reason) {
        return new VisionObservation(evidenceId, fileId, null, AssessedSeverity.INCONCLUSIVE, reason, null, List.of(), null, null);
    }

    public VisionObservation withIds(Long evidenceId, Long fileId) {
        return new VisionObservation(evidenceId, fileId, defectType, severity, severityReason, location, visibleSigns,
                activeWaterOrDampness, extent);
    }
}
