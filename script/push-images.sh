#!/bin/bash

REGISTRY="sik2dev/p-14650-2"
SERVICES=("member-service" "post-service" "payout-service" "cash-service" "market-service")

echo ""
echo "Pushing images to $REGISTRY..."

for SERVICE in "${SERVICES[@]}"; do
    echo ""
    echo "Processing $SERVICE..."
    docker compose build $SERVICE

    docker push "$REGISTRY:$SERVICE"

    if [ $? -eq 0 ]; then
        echo "$SERVICE pushed successfully"
    else
        echo "Failed to push $SERVICE"
        exit 1
    fi
done

echo ""
echo "All images pushed successfully!"
echo ""
echo "Images:"
for SERVICE in "${SERVICES[@]}"; do
    echo "  - $REGISTRY:$SERVICE"
done
