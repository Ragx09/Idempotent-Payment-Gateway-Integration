/**
 * k6 load profile for Idempotent Payment Gateway Integration:
 * 200 VUs * 20 iterations = 4,000+ requests with unique Idempotency-Key values.
 * Exercises Redis distributed locks + Postgres pessimistic locking under concurrency.
 *
 * Set account_id to a real account UUID before running: k6 run stress_test.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {
        consistency_burst: {
            executor: 'per-vu-iterations',
            vus: 200,
            iterations: 20,
            maxDuration: '2m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<2000'],
    },
};

const account_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';
const base_url = 'http://localhost:8080';

export default function () {
    const url = `${base_url}/transactions`;

    const payload = JSON.stringify({
        accountId: account_id,
        amount: 1.00,
        type: 'DEBIT',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-Key': `k6-stress-${uuidv4()}`,
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    });
}
