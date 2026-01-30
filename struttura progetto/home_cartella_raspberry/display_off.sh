#!/bin/bash
export DISPLAY=:0
export XAUTHORITY=/home/pi/.Xauthority

# Spegne brutalmente entrambe le uscite, indipendentemente dal loro stato
xrandr --output HDMI-1 --off
xrandr --output HDMI-2 --off 2>/dev/null

echo "Comando di spegnimento inviato."

