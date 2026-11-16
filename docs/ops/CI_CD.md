# CI/CD và Render release

## Workflow source

Các file trong `ci/` là nguồn bàn giao. Người dùng chép nguyên file:

```text
ci/backend-ci.yml        -> .github/workflows/backend-ci.yml
ci/backend-nightly.yml   -> .github/workflows/backend-nightly.yml
ci/deploy-staging.yml    -> .github/workflows/deploy-staging.yml
ci/deploy-production.yml -> .github/workflows/deploy-production.yml
```

Không chép `ci/mobile-ci.yml` hoặc sửa workflow mobile trong gói này.

## Gates

- PR/main: migration immutability, Java 21 `clean verify`, secret/dependency scan, runtime+migration image, fail-fast thiếu secret, container smoke với PostgreSQL 17, SBOM và High/Critical image scan.
- Nightly: full multi-device simulation, benchmark, migration→logical dump→restore DB mới, image scan kể cả unfixed.
- Staging manual: build/push hai image cùng commit, deploy digest migration service, one-off migration job, deploy runtime digest, readiness.
- Production manual: GitHub environment approval; tải manifest từ run staging, chứng minh đúng hai digest, migration rồi deploy. Rollback chỉ runtime digest cũ, không down migration.

## GitHub configuration — chỉ tên

Staging/production environments cần:

- secret `RENDER_API_KEY`;
- variable `RENDER_MIGRATION_SERVICE_ID`;
- variable `RENDER_RUNTIME_SERVICE_ID`;
- variable `HEALTH_URL`.

Production environment phải có required reviewers và không cho branch tùy ý. Render services tắt auto-deploy để workflow kiểm soát đúng digest. GHCR package phải cho hai Render service pull image.

## Render API behavior

`deploy/scripts/render-deploy.sh`:

1. deploy migration image digest và chờ `live`;
2. tạo one-off job `migrate`, poll đến `succeeded`;
3. deploy runtime image digest, poll đến `live`;
4. poll readiness.

Script retry có giới hạn cho lỗi HTTP tạm thời nhưng không tự retry migration sau trạng thái `failed`. API key không in ra output. Nguồn chính thức: [Render deploys](https://render.com/docs/deploys), [one-off job API](https://api-docs.render.com/reference/post-job), [retrieve job](https://api-docs.render.com/reference/retrieve-job), [trigger deploy](https://api-docs.render.com/reference/create-deploy), [retrieve deploy](https://api-docs.render.com/reference/retrieve-deploy).

## Rollback

- Chọn mode `rollback`, nhập digest runtime trước đã biết tốt; workflow bỏ migration.
- Nếu schema additive tương thích, app cũ phục vụ lại.
- Nếu migration có dấu hiệu phá dữ liệu/không tương thích: dừng, restore-to-new-DB theo `BACKUP_RESTORE.md`; không deploy lặp và không `flyway repair`.

## Trạng thái bằng chứng

Cho tới khi các file được chép vào `.github/workflows/` và có run URL, mọi workflow là `CHƯA CHẠY`; YAML hợp lệ không đồng nghĩa pipeline đã chạy.
