#!/usr/bin/env bash
set -euo pipefail

# 源码与部署配置分别位于 builder/mini-erp-api 和 services/mini-erp-api。
build_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
service_dir="$(cd -- "$build_dir/../../services/mini-erp-api" && pwd)"

compose() {
    # 在部署目录读取 Compose 和 .env，不受调用脚本时所在目录影响。
    (cd -- "$service_dir" && docker compose --file docker-compose.yaml "$@")
}

start_service() {
    compose up -d --no-deps --no-build --pull never --wait --wait-timeout 120 mini-erp
}

# 配置与 Docker 不可用时，在构建和替换容器前退出。
docker compose version >/dev/null
docker info >/dev/null
compose config --quiet
app_image="$(compose config --images mini-erp)"
if [[ -z "$app_image" || "$app_image" == *$'\n'* || "$app_image" == *@* ]]; then
    printf '%s\n' 'mini-erp 必须配置一个可构建的镜像名称，不能使用镜像摘要。' >&2
    exit 1
fi

previous_container="$(compose ps --all --quiet mini-erp)"
previous_image=""
rollback_image=""
if [[ -n "$previous_container" ]]; then
    previous_image="$(docker inspect --format '{{.Image}}' "$previous_container")"
    # 独立标签保留旧镜像，更新失败时自动回滚，成功后也不立即删除。
    rollback_image="mini-erp:rollback-$(date -u +%Y%m%d%H%M%S)-$$"
    docker image tag "$previous_image" "$rollback_image"
fi

printf '开始构建镜像：%s\n' "$app_image"
docker build --progress=plain -t "$app_image" "$build_dir"

# 构建成功后才替换应用容器，等待 Compose 中的 HTTP 健康检查通过。
if start_service; then
    printf '部署成功：%s\n' "$app_image"
    if [[ -n "$rollback_image" ]]; then
        printf '已保留旧镜像：%s\n' "$rollback_image"
    fi
    exit 0
fi

printf '%s\n' '部署失败，输出最近的应用日志。' >&2
compose logs --no-color --tail 100 mini-erp >&2 || true
if [[ -n "$previous_image" ]]; then
    printf '正在回滚到旧镜像：%s\n' "$rollback_image" >&2
    if docker image tag "$previous_image" "$app_image" && start_service; then
        printf '%s\n' '旧版本已恢复，本次部署仍以失败状态退出。' >&2
    else
        printf '自动回滚失败，请检查容器；旧镜像保留为：%s\n' "$rollback_image" >&2
    fi
else
    printf '%s\n' '首次部署没有旧容器可回滚，请根据日志排查。' >&2
fi
exit 1
