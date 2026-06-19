package org.example.delegate.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.AdType;
import org.example.entity.MethodStatus;
import org.example.entity.Monetization;
import org.example.entity.MonetizationMethod;
import org.example.entity.MonetizationType;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationMethodRepository;
import org.example.repository.MonetizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("saveAdPendingReviewDelegate")
@RequiredArgsConstructor
public class SaveAdPendingReviewDelegate implements JavaDelegate {

    private final MonetizationMethodRepository methodRepository;
    private final MonetizationRepository monetizationRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long monetizationId = ((Number) execution.getVariable("monetizationId")).longValue();
        String adTypeStr = (String) execution.getVariable("adType");
        String adName = (String) execution.getVariable("adName");
        String tags = (String) execution.getVariable("methodTags");

        Monetization monetization = monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));

        MonetizationMethod method = new MonetizationMethod();
        method.setMonetization(monetization);
        method.setType(MonetizationType.AD);
        method.setAdType(adTypeStr != null ? AdType.valueOf(adTypeStr) : null);
        method.setAdName(adName);
        method.setTags(tags);
        method.setStatus(MethodStatus.PENDING_REVIEW);

        method = methodRepository.save(method);
        execution.setVariable("methodId", method.getId());
        log.info("AD method id={} saved with PENDING_REVIEW", method.getId());
    }
}
