package org.example.delegate.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.Monetization;
import org.example.entity.MonetizationStatus;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component("checkMonetizationApprovedDelegate")
@RequiredArgsConstructor
public class CheckMonetizationApprovedDelegate implements JavaDelegate {

    private final MonetizationRepository monetizationRepository;

    @Override
    public void execute(DelegateExecution execution) {
        Long monetizationId = ((Number) execution.getVariable("monetizationId")).longValue();

        Monetization monetization = monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));

        if (monetization.getStatus() != MonetizationStatus.APPROVED) {
            throw new BusinessException(
                    "Монетизация не одобрена. Статус: " + monetization.getStatus(),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        log.info("Monetization id={} is approved, proceeding with method addition", monetizationId);
    }
}
