#!/bin/bash

USER_DATA_DIR="/dev/shm/chromium-kiosk-$(date +%s%N)"
mkdir -p "$USER_DATA_DIR"

export DISPLAY=:0

KIOSK_URL="~/timer_client_kiosk/timer.html" 

chromium \
    --kiosk \
    --incognito \
    --disable-infobars \
    --noerrdialogs \
    --check-for-update-at-startup=0 \
    --no-sandbox \
    --disable-features=TranslateUI \
    --disk-cache-size=1 \
    --media-cache-size=1 \
    --user-data-dir="$USER_DATA_DIR" \
    "${KIOSK_URL}" &
