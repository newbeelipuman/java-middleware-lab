package study.middleware.redisspecialization.schedule;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import study.middleware.redisspecialization.cache.CachePolicyType;
import study.middleware.redisspecialization.invalidation.InvalidationTaskProcessor;

@RestController
@RequestMapping("/api/schedules")
class ScheduleController {

    private final ScheduleQueryService queryService;
    private final ScheduleCommandService commandService;
    private final InvalidationTaskProcessor invalidationProcessor;

    ScheduleController(
            ScheduleQueryService queryService,
            ScheduleCommandService commandService,
            InvalidationTaskProcessor invalidationProcessor
    ) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.invalidationProcessor = invalidationProcessor;
    }

    @GetMapping("/{id}")
    StaffSchedule find(
            @PathVariable long id,
            @RequestParam(defaultValue = "CACHE_ASIDE") CachePolicyType policy
    ) {
        return queryService.find(id, policy);
    }

    @PutMapping("/{id}")
    StaffSchedule update(@PathVariable long id, @Valid @RequestBody UpdateScheduleRequest request) {
        ScheduleUpdateResult result = commandService.update(id, request);
        invalidationProcessor.process(result.invalidationTaskId());
        return result.schedule();
    }
}
