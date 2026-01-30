#!/bin/bash
export DISPLAY=:0
export XAUTHORITY=/home/pi/.Xauthority

xrandr --output HDMI-1 --auto
xrandr --output HDMI-2 --auto 2>/dev/null

echo "Comando di accensione inviato."

