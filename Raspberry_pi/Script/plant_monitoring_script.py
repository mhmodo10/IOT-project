import RPi.GPIO as gpio
import time
import urllib.request

#this function reads the input from the moisture sensor and writes to the thingspeak channel
def updateChannel():
    val = gpio.input(17)
    url = "https://api.thingspeak.com/update?api_key=7HOTA51FMDJLZB5O&field1="
    url_with_value = url + str(val)
    print(url_with_value)
    sent_data = urllib.request.urlopen(url_with_value)
    print(sent_data)
    print("Sensor data: "+str(val))

#setup the mode of the GPIO
gpio.setmode(gpio.BCM)
#define the input pin
gpio.setup(17, gpio.IN)
#call the function to update the thingspeak channel



#keeps the script running and calls the function every 60 seconds
while True:
    time.sleep(300)
    updateChannel()