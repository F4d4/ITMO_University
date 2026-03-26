package org.example.service;

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
import org.springframework.transaction.annotation.Transactional;

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

    // ─── BPMN 3: Запрос на монетизацию ───────────────────────────────────────────

    /**
     * (BPMN 3): Клиент (автор) выбирает стратегию монетизации и настраивает её.
     * Сервер проверяет соответствие монетизации: у пользователя есть хотя бы одно
     * опубликованное видео.
     * При успехе: монетизация одобряется и сохраняется в БД.
     * Клиент получает уведомление об одобрении.
     */
    @Transactional
    public MonetizationResponse requestMonetization(MonetizationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", request.getUserId()));

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

        // BPMN 3: «Проверка соответствия монетизации» —
        // у пользователя должно быть хотя бы одно опубликованное видео
        boolean hasPublishedVideo = videoRepository.existsByUserIdAndStatus(user.getId(), VideoStatus.PUBLISHED);
        if (!hasPublishedVideo) {
            log.warn("Пользователь id={} не имеет опубликованных видео — монетизация отклонена", user.getId());
            throw new BusinessException(
                    "Монетизация отклонена: у пользователя нет ни одного опубликованного видео. " +
                    "Сначала опубликуйте хотя бы одно видео.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        // BPMN 3: «Одобрение монетизации» + «Добавление информации в базу данных»
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
     * (BPMN 4): Клиент нажимает «Добавить рекламу» и выбирает тип монетизации.
     *
     * Сервер:
     * 1. Проверяет, что монетизация одобрена (APPROVED).
     *    Если нет → отказ («Отказ в способе монетизации»).
     * 2. Если тип AD:
     *    - Проводит модерацию названия рекламы (проверка запрещённых слов).
     *    - Если не прошло → отказ.
     *    - Требует указать adType (PRE_ROLL / MID_ROLL / POST_ROLL).
     * 3. Если тип SUBSCRIPTION:
     *    - Требует указать subscriptionPrice > 0.
     * 4. Сохраняет способ монетизации в БД.
     */
    @Transactional
    public MonetizationMethodResponse addMonetizationMethod(Long monetizationId,
                                                             MonetizationMethodRequest request) {
        Monetization monetization = monetizationRepository.findById(monetizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Монетизация", monetizationId));

        // BPMN 4: «Проверка одобрена ли монетизация»
        if (monetization.getStatus() != MonetizationStatus.APPROVED) {
            log.warn("Попытка добавить способ монетизации к неодобренной монетизации id={}, статус={}",
                    monetizationId, monetization.getStatus());
            throw new BusinessException(
                    "Монетизация не одобрена. Текущий статус: " + monetization.getStatus() +
                    ". Добавить способ монетизации можно только при статусе APPROVED.",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        MonetizationMethod method = new MonetizationMethod();
        method.setMonetization(monetization);
        method.setType(request.getType());

        if (request.getType() == MonetizationType.AD) {
            // BPMN 4: ветка «Выбрана реклама»

            if (request.getAdType() == null) {
                throw new BusinessException(
                        "Для типа AD необходимо указать тип рекламы: PRE_ROLL, MID_ROLL или POST_ROLL"
                );
            }

            // BPMN 4: «Модерация рекламы» — проверка названия на запрещённые слова
            validationService.moderateAdName(request.getAdName());

            method.setAdType(request.getAdType());
            method.setAdName(request.getAdName());

        } else if (request.getType() == MonetizationType.SUBSCRIPTION) {
            // BPMN 4: ветка «Не выбрана реклама» → подписка

            if (request.getSubscriptionPrice() == null ||
                    request.getSubscriptionPrice().doubleValue() <= 0) {
                throw new BusinessException(
                        "Для типа SUBSCRIPTION необходимо указать цену подписки (subscriptionPrice > 0)"
                );
            }

            method.setSubscriptionPrice(request.getSubscriptionPrice());
        }

        // BPMN 4: «Сохранения способа монетизации в базе данных»
        method = monetizationMethodRepository.save(method);
        log.info("Способ монетизации id={} добавлен к монетизации id={}, тип={}",
                method.getId(), monetizationId, request.getType());

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
                m.getCreatedAt()
        );
    }
}
