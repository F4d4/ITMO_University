package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.MonetizationMethodRequest;
import org.example.dto.request.MonetizationRequest;
import org.example.dto.response.MonetizationMethodResponse;
import org.example.dto.response.MonetizationResponse;
import org.example.entity.*;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.MonetizationMethodRepository;
import org.example.repository.MonetizationRepository;
import org.example.repository.UserRepository;
import org.example.repository.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonetizationService {

    private final MonetizationRepository monetizationRepository;
    private final MonetizationMethodRepository monetizationMethodRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final ValidationService validationService;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ─── BPMN 3: Запрос на монетизацию ───────────────────────────────────────────

    /**
     * BPMN 3: Клиент выбирает стратегию монетизации и настраивает её.
     * Сервер проверяет соответствие: у пользователя должно быть хотя бы одно опубликованное видео.
     * При успехе: монетизация одобряется и сохраняется в БД.
     */
    @Transactional
    public MonetizationResponse requestMonetization(Long userId, MonetizationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", userId));

        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Видео", request.getVideoId()));

        if (!video.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Видео не принадлежит данному пользователю", HttpStatus.FORBIDDEN);
        }

        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw new BusinessException(
                    "Монетизацию можно запросить только для опубликованного видео. " +
                    "Текущий статус: " + video.getStatus()
            );
        }

        if (monetizationRepository.existsByVideoIdAndStatus(video.getId(), MonetizationStatus.APPROVED)) {
            throw new BusinessException("Монетизация для данного видео уже одобрена");
        }

        // BPMN 3: Проверка соответствия монетизации
        boolean hasPublishedVideo = videoRepository.existsByUserIdAndStatus(user.getId(), VideoStatus.PUBLISHED);
        if (!hasPublishedVideo) {
            log.warn("Пользователь id={} не имеет опубликованных видео — монетизация отклонена", user.getId());
            throw new BusinessException(
                    "Монетизация отклонена: у пользователя нет ни одного опубликованного видео.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        // BPMN 3: Одобрение монетизации + добавление в БД
        Monetization monetization = new Monetization();
        monetization.setVideo(video);
        monetization.setUser(user);
        monetization.setStrategy(request.getStrategy());
        monetization.setConfiguration(request.getConfiguration());
        monetization.setStatus(MonetizationStatus.APPROVED);

        monetization = monetizationRepository.save(monetization);
        log.info("Монетизация id={} одобрена для видео id={}, пользователь id={}",
                monetization.getId(), video.getId(), user.getId());

        return toResponse(monetization);
    }

    // ─── BPMN 4: Добавление способа монетизации ──────────────────────────────────

    /**
     * BPMN 4, Шаг 1 (USER): Клиент выбирает тип монетизации (реклама или подписка) и настраивает параметры.
     * Сервер автоматически проверяет описание и валидирует параметры.
     * При успехе: способ монетизации сохраняется со статусом PENDING_REVIEW (ждёт проверки модератора).
     */
    @Transactional
    public MonetizationMethodResponse addMonetizationMethod(Long monetizationId,
                                                             MonetizationMethodRequest request) {
        Monetization monetization = monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));

        // BPMN 4: Проверка одобрена ли монетизация
        if (monetization.getStatus() != MonetizationStatus.APPROVED) {
            log.warn("Попытка добавить способ монетизации к неодобренной монетизации id={}", monetizationId);
            throw new BusinessException(
                    "Монетизация не одобрена. Текущий статус: " + monetization.getStatus(),
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        MonetizationMethod method = new MonetizationMethod();
        method.setMonetization(monetization);
        method.setType(request.getType());
        method.setTags(request.getTags());

        if (request.getType() == MonetizationType.AD) {
            if (request.getAdType() == null) {
                throw new BusinessException(
                        "Для типа AD необходимо указать тип рекламы: PRE_ROLL, MID_ROLL или POST_ROLL"
                );
            }
            // BPMN 4: Автоматическая проверка названия рекламы
            validationService.moderateAdName(request.getAdName());
            method.setAdType(request.getAdType());
            method.setAdName(request.getAdName());
            // Реклама отправляется на ручную проверку модератором
            method.setStatus(MethodStatus.PENDING_REVIEW);
            method = monetizationMethodRepository.save(method);
            log.info("Способ монетизации AD id={} добавлен со статусом PENDING_REVIEW, ждёт проверки модератором", method.getId());

        } else if (request.getType() == MonetizationType.SUBSCRIPTION) {
            // BPMN 4: Подписка — автоматическая проверка системой (цена >= 0 и не пустая)
            if (request.getSubscriptionPrice() == null) {
                throw new BusinessException(
                        "Для типа SUBSCRIPTION необходимо указать цену подписки"
                );
            }
            if (request.getSubscriptionPrice().doubleValue() < 0) {
                throw new BusinessException(
                        "Цена подписки не может быть отрицательной"
                );
            }
            method.setSubscriptionPrice(request.getSubscriptionPrice());
            // Подписка автоматически одобряется системой
            method.setStatus(MethodStatus.APPROVED);
            method = monetizationMethodRepository.save(method);
            log.info("Способ монетизации SUBSCRIPTION id={} автоматически одобрен системой", method.getId());
        }

        return toMethodResponse(method);
    }

    /**
     * BPMN 4, Шаг 2 (MODERATOR): Ручная проверка и одобрение способа монетизации.
     * Программная JTA-транзакция (Narayana):
     *   1. Сохранить способы монетизации (статус → APPROVED)
     *   2. Создать теги
     *   3. Добавить запись в БД монетизаций
     */
    public MonetizationMethodResponse approveMonetizationMethod(Long methodId) {
        MonetizationMethod method = monetizationMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("Способ монетизации", methodId));

        if (method.getStatus() != MethodStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "Одобрить можно только способ со статусом PENDING_REVIEW. " +
                    "Текущий статус: " + method.getStatus()
            );
        }

        // Программная JTA-транзакция: сохранить способы + создать теги + добавить запись в БД монетизаций
        MonetizationMethodResponse result = transactionTemplate.execute(status -> {
            MonetizationMethod m = monetizationMethodRepository.findById(methodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Способ монетизации", methodId));

            // Сохранить способы монетизации (статус APPROVED)
            m.setStatus(MethodStatus.APPROVED);

            // Создать теги (если не указаны — генерируем по умолчанию на основе типа)
            if (m.getTags() == null || m.getTags().isBlank()) {
                String autoTags = m.getType() == MonetizationType.AD
                        ? "реклама," + (m.getAdType() != null ? m.getAdType().name().toLowerCase() : "")
                        : "подписка,subscription";
                m.setTags(autoTags);
            }

            // Добавить запись в БД монетизаций
            MonetizationMethod saved = monetizationMethodRepository.save(m);
            log.info("Способ монетизации id={} одобрен модератором (JTA-транзакция)", methodId);
            // Маппинг внутри транзакции, чтобы lazy-relations были доступны
            return toMethodResponse(saved);
        });

        return result;
    }

    /**
     * BPMN 4 (MODERATOR): Отклонение способа монетизации.
     */
    @Transactional
    public MonetizationMethodResponse rejectMonetizationMethod(Long methodId, String reason) {
        MonetizationMethod method = monetizationMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("Способ монетизации", methodId));

        if (method.getStatus() != MethodStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "Отклонить можно только способ со статусом PENDING_REVIEW. " +
                    "Текущий статус: " + method.getStatus()
            );
        }

        method.setStatus(MethodStatus.REJECTED);
        method = monetizationMethodRepository.save(method);
        log.info("Способ монетизации id={} отклонён. Причина: {}", methodId, reason);

        return toMethodResponse(method);
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MonetizationResponse getMonetizationById(Long monetizationId) {
        Monetization monetization = monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));
        return toResponse(monetization);
    }

    @Transactional(readOnly = true)
    public List<MonetizationResponse> getMonetizationsByVideo(Long videoId) {
        return monetizationRepository.findByVideoId(videoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonetizationResponse> getMonetizationsByUser(Long userId) {
        return monetizationRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonetizationMethodResponse> getMethodsByMonetization(Long monetizationId) {
        monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));
        return monetizationMethodRepository.findByMonetizationId(monetizationId).stream()
                .map(this::toMethodResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonetizationMethodResponse> getPendingMethods() {
        return monetizationMethodRepository.findByStatus(MethodStatus.PENDING_REVIEW).stream()
                .map(this::toMethodResponse)
                .toList();
    }

    private MonetizationResponse toResponse(Monetization m) {
        return new MonetizationResponse(
                m.getId(),
                m.getVideo().getId(),
                m.getUser().getId(),
                m.getStrategy(),
                m.getConfiguration(),
                m.getStatus(),
                m.getCreatedAt()
        );
    }

    private MonetizationMethodResponse toMethodResponse(MonetizationMethod m) {
        return new MonetizationMethodResponse(
                m.getId(),
                m.getMonetization().getId(),
                m.getType(),
                m.getAdType(),
                m.getAdName(),
                m.getSubscriptionPrice(),
                m.getStatus(),
                m.getTags(),
                m.getCreatedAt()
        );
    }
}
