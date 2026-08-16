#!/bin/bash

# One-command control of the local Kubernetes deployment.
#
#   ./cluster.sh up        build images, deploy, wait for ready, seed demo data
#   ./cluster.sh down      remove the app, keep the databases' data
#   ./cluster.sh destroy   remove everything including the data volumes
#   ./cluster.sh status    what is running, and on which ports
#
#   SKIP_BUILD=1 ./cluster.sh up    redeploy without rebuilding images
#   SKIP_SEED=1  ./cluster.sh up    deploy without demo data

set -euo pipefail

cd "$(dirname "$0")"

SERVICES=(user-service product-service cart-service order-service payment-service)
DEPLOYMENTS=(user-service product-service cart-service order-service payment-service frontend)

require_cluster() {
    kubectl cluster-info >/dev/null 2>&1 || {
        echo "No Kubernetes cluster reachable."
        echo "Enable it in Docker Desktop: Settings -> Kubernetes -> Enable Kubernetes -> Apply & Restart"
        exit 1
    }
}

up() {
    require_cluster

    # A fresh tag per deploy is not cosmetic: the cluster node caches images by tag, so re-applying
    # an unchanged tag silently keeps serving the previously loaded build.
    local tag="build-$(date +%s)"

    if [ "${SKIP_BUILD:-0}" = "1" ]; then
        echo "Skipping image build (SKIP_BUILD=1)"
        tag=""
    else
        echo "Building images ($tag)"
        for service in "${SERVICES[@]}"; do
            printf '  %-18s' "$service"
            docker build -q -t "ecommerce/${service}:${tag}" "backend/${service}" >/dev/null
            echo "ok"
        done
        printf '  %-18s' "frontend"
        docker build -q -t "ecommerce/frontend:${tag}" frontend >/dev/null
        echo "ok"
    fi

    echo
    echo "Applying manifests"
    kubectl apply -f k8s/ | sed 's/^/  /'

    if [ -n "$tag" ]; then
        echo
        echo "Pointing deployments at $tag"
        for deployment in "${DEPLOYMENTS[@]}"; do
            kubectl set image "deploy/${deployment}" "${deployment}=ecommerce/${deployment}:${tag}" >/dev/null
        done
    fi

    echo
    echo "Waiting for pods (databases first, then services)"
    for deployment in mysql mongodb redis "${DEPLOYMENTS[@]}"; do
        printf '  %-18s' "$deployment"
        if kubectl rollout status "deploy/${deployment}" --timeout=300s >/dev/null 2>&1; then
            echo "ready"
        else
            echo "NOT READY -- kubectl describe deploy/${deployment}"
        fi
    done

    if [ "${SKIP_SEED:-0}" != "1" ]; then
        echo
        # The services answer before MySQL has finished creating tables on a first run, so let the
        # seed script's own retry window absorb it.
        WAIT_SECONDS=120 ./seed.sh | sed 's/^/  /'
    fi

    echo
    status
}

down() {
    require_cluster
    echo "Removing the app (data volumes are kept)"
    # Named explicitly rather than --all: that form also removes the built-in kubernetes API
    # service. Deleting the Services matters as much as the pods -- LoadBalancer services hold
    # ports 3000 and 8081-8085 even with zero replicas, and anything started on the host afterwards
    # collides with a listener that has nothing behind it.
    kubectl delete deploy "${DEPLOYMENTS[@]}" mysql mongodb redis --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    kubectl delete svc "${DEPLOYMENTS[@]}" mysql mongodb redis --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    kubectl delete secret ecommerce-secrets --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    echo
    echo "Data kept in:"
    kubectl get pvc --no-headers 2>/dev/null | awk '{print "  " $1 "  " $4}' || echo "  (none)"
    echo
    echo "Bring it back with ./cluster.sh up   |   erase the data with ./cluster.sh destroy"
}

destroy() {
    require_cluster
    echo "Removing everything, including the database volumes"
    kubectl delete -f k8s/ --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    kubectl delete pvc --all --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    echo
    echo "Gone. To stop Kubernetes itself: Docker Desktop -> Settings -> Kubernetes -> disable."
}

status() {
    require_cluster
    local running
    running=$(kubectl get pods --no-headers 2>/dev/null | grep -c ' 1/1 ' || true)
    echo "Pods ready: ${running}"
    kubectl get pods --no-headers 2>/dev/null | awk '{printf "  %-34s %s %s\n", $1, $2, $3}' || true

    if [ "$running" -gt 0 ]; then
        echo
        echo "  Frontend         http://localhost:3000"
        echo "  user-service     http://localhost:8081"
        echo "  product-service  http://localhost:8082"
        echo "  cart-service     http://localhost:8083"
        echo "  order-service    http://localhost:8084"
        echo "  payment-service  http://localhost:8085"
        echo
        echo "  Log in as alice@example.com / password123"
    fi
}

case "${1:-}" in
    up) up ;;
    down) down ;;
    destroy) destroy ;;
    status) status ;;
    *)
        echo "Usage: ./cluster.sh {up|down|destroy|status}"
        exit 1
        ;;
esac
