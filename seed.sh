#!/bin/bash

# Seeds a demo catalog, demo users, and one populated cart.
#
# Safe to re-run: products already present by name are skipped, and re-registering an existing
# email is treated as "already there" rather than an error.
#
#   ./seed.sh                # localhost (docker-compose or the k8s cluster)
#   HOST=1.2.3.4 ./seed.sh

set -u

HOST="${HOST:-localhost}"
USER_URL="http://$HOST:8081"
PRODUCT_URL="http://$HOST:8082"
CART_URL="http://$HOST:8083"

PASSWORD="password123"

command -v jq >/dev/null || { echo "jq is required (brew install jq)"; exit 1; }

for target in "$USER_URL/api/users" "$PRODUCT_URL/api/products" "$CART_URL/api/cart/preflight"; do
    curl -sS -m 5 -o /dev/null "$target" 2>/dev/null || {
        echo "Cannot reach ${target%%/api/*}. Start the services first (see README)."
        exit 1
    }
done

# name|description|price|category|brand|stock|image
PRODUCTS='Laptop|High-performance 14-inch laptop|999.99|Electronics|TechBrand|12|Laptop
Smartphone|Latest smartphone with great cameras|699.99|Electronics|PhoneBrand|25|Phone
Headphones|Wireless noise-cancelling headphones|199.99|Audio|AudioBrand|40|Headphones
4K Monitor|27-inch 4K IPS display|449.00|Electronics|ViewBrand|8|Monitor
Mechanical Keyboard|Hot-swappable switches, USB-C|129.50|Electronics|TechBrand|30|Keyboard
Wireless Mouse|Ergonomic, silent click|49.99|Electronics|TechBrand|50|Mouse
Bluetooth Speaker|Portable, 12-hour battery|89.99|Audio|AudioBrand|18|Speaker
Wireless Earbuds|In-ear, active noise cancelling|149.00|Audio|AudioBrand|22|Earbuds
Espresso Machine|15-bar pump, milk frother|329.00|Home|BrewBrand|6|Espresso
Air Purifier|HEPA filter for large rooms|219.99|Home|PureBrand|10|Purifier
Standing Desk|Electric height-adjustable, 120cm|549.00|Furniture|DeskBrand|4|Desk
Desk Lamp|Dimmable LED with USB port|39.99|Furniture|LightBrand|2|Lamp
Webcam 1080p|Autofocus with privacy shutter|79.99|Electronics|TechBrand|0|Webcam'

# email|first|last|phone|address|city|country|postcode
USERS='alice@example.com|Alice|Nguyen|5550100|12 Oak Street|Chicago|USA|60601
bob@example.com|Bob|Martins|5550101|48 Pine Avenue|Austin|USA|73301
carol@example.com|Carol|Silva|5550102|9 Cedar Road|Seattle|USA|98101
dave@example.com|Dave|Okafor|5550103|77 Birch Lane|Boston|USA|02108'

echo "Products"
existing=$(curl -sS -m 10 "$PRODUCT_URL/api/products/active?page=0&size=200" | jq -r '.content[]?.name')
created=0
skipped=0
while IFS='|' read -r name description price category brand stock image; do
    [ -z "$name" ] && continue
    if grep -Fxq "$name" <<<"$existing"; then
        skipped=$((skipped + 1))
        continue
    fi
    body=$(jq -n --arg n "$name" --arg d "$description" --argjson p "$price" --arg c "$category" \
                 --arg b "$brand" --argjson s "$stock" --arg i "https://placehold.co/300x200?text=$image" \
                 '{name:$n, description:$d, price:$p, category:$c, brand:$b, stockQuantity:$s, imageUrl:$i}')
    code=$(curl -sS -m 10 -o /dev/null -w '%{http_code}' -X POST "$PRODUCT_URL/api/products" \
        -H 'Content-Type: application/json' -d "$body")
    if [ "$code" = "201" ]; then
        created=$((created + 1))
    else
        echo "  failed to create $name (HTTP $code)"
    fi
done <<<"$PRODUCTS"
echo "  $created created, $skipped already present"

echo
echo "Users (password for all: $PASSWORD)"
registered=0
existed=0
while IFS='|' read -r email first last phone address city country postcode; do
    [ -z "$email" ] && continue
    body=$(jq -n --arg e "$email" --arg p "$PASSWORD" --arg f "$first" --arg l "$last" --arg ph "$phone" \
                 --arg a "$address" --arg c "$city" --arg co "$country" --arg pc "$postcode" \
                 '{email:$e, password:$p, firstName:$f, lastName:$l, phoneNumber:$ph,
                   address:$a, city:$c, country:$co, postalCode:$pc}')
    code=$(curl -sS -m 10 -o /dev/null -w '%{http_code}' -X POST "$USER_URL/api/users/register" \
        -H 'Content-Type: application/json' -d "$body")
    case "$code" in
        201) registered=$((registered + 1)); echo "  $email" ;;
        409) existed=$((existed + 1)) ;;
        *) echo "  failed to register $email (HTTP $code)" ;;
    esac
done <<<"$USERS"
echo "  $registered registered, $existed already present"

echo
echo "Cart for alice@example.com"
login=$(curl -sS -m 10 -X POST "$USER_URL/api/users/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"alice@example.com\",\"password\":\"$PASSWORD\"}")
token=$(jq -r '.token // empty' <<<"$login")
user_id=$(jq -r '.user.id // empty' <<<"$login")

if [ -z "$token" ]; then
    echo "  could not log in, skipping cart seed"
else
    current=$(curl -sS -m 10 "$CART_URL/api/cart/$user_id" -H "Authorization: Bearer $token" | jq -r '.totalItems // 0')
    if [ "$current" -gt 0 ]; then
        echo "  already has $current item(s), leaving it alone"
    else
        catalog=$(curl -sS -m 10 "$PRODUCT_URL/api/products/active?page=0&size=200")
        for wanted in "Laptop" "Wireless Mouse"; do
            pid=$(jq -r --arg n "$wanted" '.content[] | select(.name == $n) | .id' <<<"$catalog" | head -1)
            [ -z "$pid" ] && continue
            curl -sS -m 10 -o /dev/null -X POST "$CART_URL/api/cart/$user_id/items" \
                -H 'Content-Type: application/json' -H "Authorization: Bearer $token" \
                -d "{\"productId\":\"$pid\",\"quantity\":1}"
            echo "  added $wanted"
        done
    fi
fi

echo
echo "Done. Log in at http://$HOST:3000 as any seeded user with password $PASSWORD"
