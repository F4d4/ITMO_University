package org.example.delegate.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.example.entity.MonetizationMethod;
import org.example.entity.MonetizationType;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationMethodRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("saveAdTagsDelegate")
@RequiredArgsConstructor
public class SaveAdTagsDelegate implements JavaDelegate {

    private final MonetizationMethodRepository methodRepository;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) {
        Long methodId = ((Number) execution.getVariable("methodId")).longValue();

        MonetizationMethod method = methodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("Способ монетизации", methodId));

        if (method.getTags() == null || method.getTags().isBlank()) {
            String autoTags = method.getType() == MonetizationType.AD
                    ? "реклама," + (method.getAdType() != null ? method.getAdType().name().toLowerCase() : "")
                    : "подписка,subscription";
            method.setTags(autoTags);
            methodRepository.save(method);
            log.info("Auto-generated tags for method id={}: {}", methodId, autoTags);
        }
    }
}
