package com.dwell.domain.analysis.event;

import com.dwell.domain.analysis.service.CaseAnalysisService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 케이스·증거가 커밋된 뒤에 분석 시작 (응답 스레드를 막지 않도록 가상 스레드에서)
@Component
@RequiredArgsConstructor
public class CaseAnalysisListener {

    private final CaseAnalysisService caseAnalysisService;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisRequested(CaseAnalysisRequestedEvent event) {
        executor.execute(() -> caseAnalysisService.analyze(event.caseId()));
    }
}
