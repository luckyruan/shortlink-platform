import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "10s", target: 20 },
    { duration: "30s", target: 100 },
    { duration: "10s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const code = __ENV.SHORT_CODE;

export default function () {
  if (!code) throw new Error("Set SHORT_CODE before running the test");
  const response = http.get(`${baseUrl}/r/${code}`, { redirects: 0 });
  check(response, {
    "returns redirect": (r) => r.status === 302,
    "has location": (r) => Boolean(r.headers.Location),
  });
  sleep(0.05);
}
