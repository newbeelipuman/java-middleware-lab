import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8091';

export const options = {
  scenarios: {
    create_shift: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function () {
  const body = JSON.stringify({
    staffCode: `LOAD-${__VU}-${__ITER}`,
    fromShift: 'DAY',
    toShift: 'NIGHT',
  });
  const response = http.post(`${baseUrl}/api/shift-changes`, body, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(response, { 'created': (result) => result.status === 201 });
  sleep(0.05);
}
