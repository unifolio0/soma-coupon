import http from 'k6/http';
import {check} from 'k6';

export let options = {
    vus: 250,
    iterations: 250,
};

export default function () {
    const couponId = (__VU % 3) + 1;

    const payload = JSON.stringify({
        userId: __VU,
        couponId: couponId,
    });

    const params = {
        headers: {'Content-Type': 'application/json'},
    };

    const res = http.post('http://localhost:8080/api/coupon/issue', payload, params);

    check(res, {
        '200 or 500': (r) => r.status === 200 || r.status === 500,
    });
}
