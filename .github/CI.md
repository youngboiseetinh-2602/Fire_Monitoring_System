# GitHub Actions

Workflow `CI` runs on pushes to `main`, pull requests targeting `main`, and manually
from GitHub > Actions > CI > Run workflow.

- Backend: Java 21, Maven `clean verify` (tests and JAR packaging).
- Frontend: Node 22, `npm ci`, API client tests, and Vite production build.
- After both pass: validate Compose and build both Docker images, without pushing
  images to a registry or starting services.

No GitHub secrets are required. Backend repository tests use H2; MQTT tests use
mocks. The workflow does not connect to production TiDB or use production JWT keys.
Playwright browser tests are not included in this workflow.

## Deployment

This workflow performs CI only. Render and Vercel deployments remain managed by
their GitHub integrations. Existing automatic deployments are not gated by this
workflow; a failed check does not automatically stop those deployments.

For checks before merging, configure a GitHub branch rule for `main` that requires
pull requests and the CI checks, where supported by your repository plan.

Set `VITE_API_BASE_URL` on Vercel before rebuilding the frontend and
`CORS_ALLOWED_ORIGINS` on Render to the frontend origin. A passing CI build does
not verify these hosting settings or fix an incorrectly configured API URL.

Action references: https://github.com/actions/checkout,
https://github.com/actions/setup-java, https://github.com/actions/setup-node.
