package org.example.delegate.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.MethodStatus;
import org.example.entity.Monetization;
import org.example.entity.MonetizationMethod;
import org.example.entity.MonetizationType;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationMethodRepository;
import org.example.repository.MonetizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component("saveSubscriptionMethodDelegate")
@RequiredArgsConstructor
public class SaveSubscriptionMethodDelegate implements JavaDelegate {

    private final MonetizationMethodRepository methodRepository;
    private final MonetizationRepository monetizationRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long monetizationId = ((Number) execution.getVariable("monetizationId")).longValue();
        Object priceVar = execution.getVariable("subscriptionPrice");
        String tags = (String) execution.getVariable("methodTags");

        if (priceVar == null) {
            throw new BusinessException("Цена подписки обязательна");
        }

        BigDecimal price = new BigDecimal(priceVar.toString());
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Цена подписки не может быть отрицательной");
        }

        Monetization monetization = monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));

        MonetizationMethod method = new MonetizationMethod();
        method.setMonetization(monetization);
        method.setType(MonetizationType.SUBSCRIPTION);
        method.setSubscriptionPrice(price);
        method.setTags(tags != null ? tags : "подписка,subscription");
        method.setStatus(MethodStatus.APPROVED);

        method = methodRepository.save(method);
        execution.setVariable("methodId", method.getId());
        log.info("SUBSCRIPTION method id={} saved with APPROVED status, price={}", method.getId(), price);
    }
}
