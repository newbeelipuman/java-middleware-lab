package study.middleware.redisspecialization.status;

import java.util.List;

record StatusResponse(
        String project,
        String sourceBaseline,
        String stage,
        List<String> plannedPolicies
) {
}
