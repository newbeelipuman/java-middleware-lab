import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8090';
const policy = __ENV.CACHE_POLICY || 'CACHE_ASIDE';
const scheduleId = __ENV.SCHEDULE_ID || '1';

export const options = {
  scenarios: {
    schedule_read: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  if ((__ENV.WARMUP || 'true') === 'true') {
    for (let index = 0; index < 20; index += 1) {
      http.get(`${baseUrl}/api/schedules/${scheduleId}?policy=${policy}`);
    }
  }
}

export default function () {
  const response = http.get(`${baseUrl}/api/schedules/${scheduleId}?policy=${policy}`);
  check(response, {
    'status is expected': (result) => result.status === 200 || result.status === 404 || result.status === 503,
  });
  sleep(Number(__ENV.SLEEP_SECONDS || 0.05));
}
