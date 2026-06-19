package org.example.delegate.method;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.MethodStatus;
import org.example.entity.MonetizationMethod;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationMethodRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component("approveAdMethodDelegate")
@RequiredArgsConstructor
public class ApproveAdMethodDelegate implements JavaDelegate {

    private final MonetizationMethodRepository methodRepository;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Long methodId = ((Number) execution.getVariable("methodId")).longValue();

        transactionTemplate.execute(status -> {
            MonetizationMethod method = methodRepository.findById(methodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Способ монетизации", methodId));
            method.setStatus(MethodStatus.APPROVED);
            methodRepository.save(method);
            log.info("AD method id={} approved via JTA transaction", methodId);
            return null;
        });
    }
}
