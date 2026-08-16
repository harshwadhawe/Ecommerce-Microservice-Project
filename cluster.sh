#!/bin/bash

# One-command control of the local Kubernetes deployment.
#
#   ./cluster.sh up        build images, deploy, wait for ready, seed demo data
#   ./cluster.sh down      remove the app, keep the databases' data
#   ./cluster.sh destroy   remove everything including the data volumes
#   ./cluster.sh status    what is running, and on which ports
#   ./cluster.sh watch     redeploy automatically on every source change (skaffold dev)
#
#   SKIP_SEED=1 ./cluster.sh up     deploy without demo data
#
# Building and deploying is skaffold's job (see skaffold.yaml): it tags images by content digest, so
# a rebuilt image always gets a tag the cluster has not cached. This script adds what skaffold has no
# opinion about -- seeding, the data-preserving teardown, and printing where things are listening.

set -euo pipefail

cd "$(dirname "$0")"

SERVICES=(user-service product-service cart-service order-service payment-service)
DEPLOYMENTS=(user-service product-service cart-service order-service payment-service frontend)

require_skaffold() {
    command -v skaffold >/dev/null || {
        echo "skaffold is not installed: brew install skaffold"
        exit 1
    }
}

require_cluster() {
    kubectl cluster-info >/dev/null 2>&1 || {
        echo "No Kubernetes cluster reachable."
        echo "Enable it in Docker Desktop: Settings -> Kubernetes -> Enable Kubernetes -> Apply & Restart"
        exit 1
    }
}

up() {
    require_cluster
    require_skaffold

    echo "Building and deploying (skaffold)"
    # `run` builds every artifact, rewrites the image references in k8s/ to content-digest tags and
    # applies them. Waiting is done below rather than by skaffold: restarting a database briefly
    # crash-loops the services that depend on it, which is recovery, not failure.
    skaffold run 2>&1 | grep -vE '^\s+>|Waited before sending request' | sed 's/^/  /'

    echo
    echo "Waiting for rollout"
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
        # Services answer before MySQL has finished creating tables on a first run; the seed script's
        # own retry window absorbs that.
        WAIT_SECONDS=120 ./seed.sh | sed 's/^/  /'
    fi

    echo
    status
}

watch() {
    require_cluster
    require_skaffold
    echo "Watching for changes -- edit a file and it redeploys. Ctrl-C to stop and tear down."
    skaffold dev
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
    if command -v skaffold >/dev/null; then
        skaffold delete 2>&1 | sed 's/^/  /' || true
    else
        kubectl delete -f k8s/ --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    fi
    kubectl delete pvc --all --ignore-not-found 2>/dev/null | sed 's/^/  /' || true
    echo
    echo "Gone. To stop Kubernetes itself: Docker Desktop -> Settings -> Kubernetes -> disable."
}

status() {
    require_cluster
    local running
    running=$(kubectl get pods --no-headers 2>/dev/null | grep -v Terminating | grep -c ' 1/1 ' || true)
    echo "Pods ready: ${running}"
    kubectl get pods --no-headers 2>/dev/null | grep -v Terminating | awk '{printf "  %-34s %s %s\n", $1, $2, $3}' || true

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
    watch) watch ;;
    down) down ;;
    destroy) destroy ;;
    status) status ;;
    *)
        echo "Usage: ./cluster.sh {up|watch|down|destroy|status}"
        exit 1
        ;;
esac
