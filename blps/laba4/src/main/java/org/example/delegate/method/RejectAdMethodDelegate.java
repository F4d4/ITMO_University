package org.example.delegate.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.MethodStatus;
import org.example.entity.MonetizationMethod;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationMethodRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("rejectAdMethodDelegate")
@RequiredArgsConstructor
public class RejectAdMethodDelegate implements JavaDelegate {

    private final MonetizationMethodRepository methodRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long methodId = ((Number) execution.getVariable("methodId")).longValue();
        String reason = (String) execution.getVariable("adRejectionReason");

        MonetizationMethod method = methodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("Способ монетизации", methodId));

        method.setStatus(MethodStatus.REJECTED);
        methodRepository.save(method);
        log.info("AD method id={} rejected. Reason: {}", methodId, reason);
    }
}
