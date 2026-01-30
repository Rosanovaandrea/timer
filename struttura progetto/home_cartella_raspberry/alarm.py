import sys
import logging
from gpiozero import OutputDevice, GPIOZeroError
from time import sleep

# Configurazione fissa
PIN_GPIO = 24

def main():
    # Controlla se è stato passato un argomento, altrimenti usa 10
    durata = int(sys.argv[1]) if len(sys.argv) > 1 else 10

    try:
        alarm = OutputDevice(PIN_GPIO, active_high=True, initial_value=False)
        alarm.on()
        sleep(durata)
        alarm.off()
    except GPIOZeroError:
        print("Errore GPIO")
    except Exception as e:
        print(f"Errore: {e}")

if __name__ == "__main__":
    main()
