#!/bin/bash

# End-to-end API test. Exercises the real HTTP contracts across services.
#
# Prerequisites: user, product, cart and payment services running (see README).
# Order-service is not covered because it is not implemented.
#
#   ./test-e2e.sh            # localhost
#   HOST=1.2.3.4 ./test-e2e.sh

set -u

HOST="${HOST:-localhost}"
USER_URL="http://$HOST:8081"
PRODUCT_URL="http://$HOST:8082"
CART_URL="http://$HOST:8083"
PAYMENT_URL="http://$HOST:8085"

passed=0
failed=0

# check <name> <expected> <actual>
check() {
    if [ "$2" = "$3" ]; then
        echo "  ok    $1"
        passed=$((passed + 1))
    else
        echo "  FAIL  $1 (expected '$2', got '$3')"
        failed=$((failed + 1))
    fi
}

# call <method> <url> [json] -> sets $STATUS and $BODY
call() {
    local response
    if [ $# -ge 3 ]; then
        response=$(curl -sS -m 15 -w '\n%{http_code}' -X "$1" "$2" \
            -H 'Content-Type: application/json' ${TOKEN:+-H "Authorization: Bearer $TOKEN"} -d "$3" 2>/dev/null)
    else
        response=$(curl -sS -m 15 -w '\n%{http_code}' -X "$1" "$2" \
            ${TOKEN:+-H "Authorization: Bearer $TOKEN"} 2>/dev/null)
    fi
    STATUS="${response##*$'\n'}"
    BODY="${response%$'\n'*}"
}

# Waits up to WAIT_SECONDS for a service to answer. CI sets this high enough to cover JVM startup.
WAIT_SECONDS="${WAIT_SECONDS:-5}"
require() {
    local deadline=$((SECONDS + WAIT_SECONDS))
    while true; do
        curl -sS -m 5 -o /dev/null "$1" 2>/dev/null && return 0
        [ "$SECONDS" -ge "$deadline" ] && break
        sleep 2
    done
    echo "Cannot reach $2 at $1 (waited ${WAIT_SECONDS}s)"
    echo "Start it with: mvn -f backend/$3/pom.xml spring-boot:run"
    exit 1
}

command -v jq >/dev/null || { echo "jq is required (brew install jq)"; exit 1; }

echo "Checking services..."
require "$USER_URL/api/users"          "user-service"    "user-service"
require "$PRODUCT_URL/api/products"    "product-service" "product-service"
require "$CART_URL/api/cart/preflight" "cart-service"    "cart-service"
require "$PAYMENT_URL/api/payment"     "payment-service" "payment-service"

TOKEN=""
EMAIL="e2e-$(date +%s)-$RANDOM@test.com"
USER_ID=""
PRODUCT_ID=""

echo
echo "Users"
call POST "$USER_URL/api/users/register" \
    "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"firstName\":\"E2E\",\"lastName\":\"Test\"}"
check "register returns 201" "201" "$STATUS"
USER_ID=$(echo "$BODY" | jq -r '.id // empty')
check "register omits password" "null" "$(echo "$BODY" | jq -r '.password')"

call POST "$USER_URL/api/users/register" \
    "{\"email\":\"$EMAIL\",\"password\":\"password123\",\"firstName\":\"E2E\",\"lastName\":\"Test\"}"
check "duplicate email returns 409" "409" "$STATUS"

call POST "$USER_URL/api/users/register" \
    "{\"email\":\"not-an-email\",\"password\":\"password123\",\"firstName\":\"E2E\",\"lastName\":\"Test\"}"
check "invalid email returns 400" "400" "$STATUS"

call POST "$USER_URL/api/users/login" "{\"email\":\"$EMAIL\",\"password\":\"wrong-password\"}"
check "wrong password returns 401" "401" "$STATUS"
check "wrong password reveals nothing" "Invalid email or password" "$(echo "$BODY" | jq -r '.error')"

call POST "$USER_URL/api/users/login" '{"email":"nobody-at-all@test.com","password":"password123"}'
check "unknown account is indistinguishable" "401" "$STATUS"

call POST "$USER_URL/api/users/register" 'not json at all'
check "malformed JSON returns 400" "400" "$STATUS"

call POST "$USER_URL/api/users/login" "{\"email\":\"$EMAIL\",\"password\":\"password123\"}"
check "login returns 200" "200" "$STATUS"
TOKEN=$(echo "$BODY" | jq -r '.token // empty')
[ -n "$TOKEN" ] && check "login returns a JWT" "3" "$(echo "$TOKEN" | awk -F. '{print NF}')"

call GET "$USER_URL/api/users/profile"
check "profile with token returns 200" "200" "$STATUS"
check "profile is the logged-in user" "$EMAIL" "$(echo "$BODY" | jq -r '.email')"

SAVED_TOKEN="$TOKEN"
TOKEN=""
call GET "$USER_URL/api/users/profile"
check "profile without token returns 401" "401" "$STATUS"
TOKEN="$SAVED_TOKEN"

echo
echo "Products"
call POST "$PRODUCT_URL/api/products" \
    '{"name":"E2E Laptop","description":"Test product","price":999.99,"category":"Electronics","brand":"Acme","stockQuantity":5}'
check "create returns 201" "201" "$STATUS"
PRODUCT_ID=$(echo "$BODY" | jq -r '.id // empty')
check "created product is active" "true" "$(echo "$BODY" | jq -r '.isActive')"

call GET "$PRODUCT_URL/api/products/$PRODUCT_ID"
check "fetch by id returns 200" "200" "$STATUS"

call GET "$PRODUCT_URL/api/products/does-not-exist"
check "unknown id returns 404" "404" "$STATUS"

call POST "$PRODUCT_URL/api/products" '{"name":"Broken","price":-5}'
check "invalid product returns 400" "400" "$STATUS"

call GET "$PRODUCT_URL/api/products/search?q=E2E%20Laptop"
check "search returns 200" "200" "$STATUS"
check "search finds the product" "true" \
    "$(echo "$BODY" | jq --arg id "$PRODUCT_ID" 'any(.content[]; .id == $id)')"

call GET "$PRODUCT_URL/api/products/active"
check "active list has no null entries" "0" "$(echo "$BODY" | jq '[.content[] | select(. == null)] | length')"

echo
echo "Cart"
CART_USER="${USER_ID:-1}"
call DELETE "$CART_URL/api/cart/$CART_USER" >/dev/null

call POST "$CART_URL/api/cart/$CART_USER/items" "{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}"
check "add to cart returns 200" "200" "$STATUS"
check "cart holds 2 items" "2" "$(echo "$BODY" | jq -r '.totalItems')"
check "cart total is 1999.98" "1999.98" "$(echo "$BODY" | jq -r '.totalAmount')"
check "cart item carries the product name" "E2E Laptop" "$(echo "$BODY" | jq -r '.items[0].productName')"

call POST "$CART_URL/api/cart/$CART_USER/items" "{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}"
check "re-adding merges into one line" "1" "$(echo "$BODY" | jq -r '.items | length')"
check "merged quantity is 4" "4" "$(echo "$BODY" | jq -r '.totalItems')"

call POST "$CART_URL/api/cart/$CART_USER/items" "{\"productId\":\"$PRODUCT_ID\",\"quantity\":99}"
check "quantity above stock returns 400" "400" "$STATUS"

call POST "$CART_URL/api/cart/$CART_USER/items" "{\"productId\":\"$PRODUCT_ID\",\"quantity\":0}"
check "zero quantity returns 400" "400" "$STATUS"

call POST "$CART_URL/api/cart/$CART_USER/items" '{"productId":"does-not-exist","quantity":1}'
check "unknown product returns 400" "400" "$STATUS"

call GET "$CART_URL/api/cart/$CART_USER/validate"
check "cart validates against stock" "true" "$(echo "$BODY" | jq -r '.valid')"

SAVED_TOKEN="$TOKEN"
TOKEN=""
call GET "$CART_URL/api/cart/$CART_USER"
check "cart without a token returns 401" "401" "$STATUS"
call DELETE "$CART_URL/api/cart/$CART_USER"
check "cart cannot be cleared without a token" "401" "$STATUS"
TOKEN="$SAVED_TOKEN"

OTHER_USER=$((CART_USER + 1))
call GET "$CART_URL/api/cart/$OTHER_USER"
check "another user's cart returns 403" "403" "$STATUS"
call DELETE "$CART_URL/api/cart/$OTHER_USER"
check "another user's cart cannot be cleared" "403" "$STATUS"

call PUT "$CART_URL/api/cart/$CART_USER/items/$PRODUCT_ID" '{"quantity":1}'
check "quantity update returns 200" "200" "$STATUS"
check "quantity is now 1" "1" "$(echo "$BODY" | jq -r '.totalItems')"

call PUT "$CART_URL/api/cart/$CART_USER/items/$PRODUCT_ID" '{"quantity":0}'
check "zero quantity removes the item" "0" "$(echo "$BODY" | jq -r '.items | length')"

call POST "$CART_URL/api/cart/$CART_USER/items" "{\"productId\":\"$PRODUCT_ID\",\"quantity\":1}"
call GET "$CART_URL/api/cart/$CART_USER"
check "cart survives a re-read" "1" "$(echo "$BODY" | jq -r '.totalItems')"

call DELETE "$CART_URL/api/cart/$CART_USER"
check "clear cart returns 200" "200" "$STATUS"
call GET "$CART_URL/api/cart/$CART_USER"
check "cleared cart is empty" "0" "$(echo "$BODY" | jq -r '.totalItems')"

echo
echo "Payment"
call POST "$PAYMENT_URL/api/payment/process" \
    '{"orderId":"e2e-order-1","amount":1999.98,"paymentMethod":"CARD","cardNumber":"4111111111111111","cvv":"123","expiryDate":"12/30","cardholderName":"E2E Test"}'
check "payment returns 200" "200" "$STATUS"
PAYMENT_STATUS=$(echo "$BODY" | jq -r '.status')
if [ "$PAYMENT_STATUS" = "SUCCESS" ] || [ "$PAYMENT_STATUS" = "FAILED" ]; then
    echo "  ok    payment status is SUCCESS or FAILED (got $PAYMENT_STATUS, simulator is random)"
    passed=$((passed + 1))
else
    echo "  FAIL  payment status is SUCCESS or FAILED (got '$PAYMENT_STATUS')"
    failed=$((failed + 1))
fi
check "payment echoes the order id" "e2e-order-1" "$(echo "$BODY" | jq -r '.orderId')"
check "payment returns a transaction id" "36" "$(echo "$BODY" | jq -r '.transactionId | length')"

call POST "$PAYMENT_URL/api/payment/process" '{"orderId":"e2e-order-2","amount":10.00}'
check "payment without card details returns 400" "400" "$STATUS"

echo
echo "Orders"
echo "  skip  order-service is not implemented; checkout cannot be tested end to end"

echo
echo "Cleanup"
TOKEN="$SAVED_TOKEN"
[ -n "$PRODUCT_ID" ] && call DELETE "$PRODUCT_URL/api/products/$PRODUCT_ID"
[ -n "$USER_ID" ] && call DELETE "$USER_URL/api/users/$USER_ID"
echo "  removed the test product and user"

echo
echo "$passed passed, $failed failed"
[ "$failed" -eq 0 ]
