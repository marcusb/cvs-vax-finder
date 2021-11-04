# cvs-vax-finder

Fork of [@danielnorberg's script](https://github.com/danielnorberg/konsulatet)
to scrape the [CVS web site](https://www.cvs.com) for Covid-19 vaccine appointments.

## Usage

1. Book an appointment on cvs.com as usual.
2. Receive the confirmation e-mail, it will contain a link for changing the appointment. Open this link in your browser (the same that you used to make the appointment).
3. Save the values of the cookies starting with `DG_`. You can copy them using the Chrome developer tools for example.
4. Get a https://www.twilio.com/ trial account, get a twilio phone number & verify the phone number
   you want to send sms to
5. `brew install maven` (if you don’t have maven + java)
6. `git clone git@github.com:marcusb/cvs-vax-finder.git`
7. `cd cvs-vax-finder`
8. Edit `AppointmentChecker.java`:
   * Set the URL to the rescheduling link from the confirmation e-mail.
   * Set your location and desired last date of interest.
   * Insert the cookie values you saved earlier.
9. `TWILIO_SID=badf00d TWILIO_TOKEN=badf00d NOTIFICATION_NUMBERS=+12222222222,+13333333333 TWILIO_NUMBER=+14444444444 ./run.sh`
10. Verify that you get the hello world SMS on startup, otherwise something is wrong and you might
    not get any notifications for appointments.

* The `TWILIO_SID`, `TWILIO_TOKEN` env vars should be set to your twilio SID and auth token.
* `TWILIO_NUMBER` should be your assigned Twilio phone number.
* `NOTIFICATION_NUMBERS` Can be one or more comma separated numbers to send SMS to. Note that the
  numbers must be verified in your Twilio account.

## Note

* The `run.sh` script tries to run the application forever and restart it on crash.
* An SMS is sent if the application crashes so you can debug and get it up and running again.
* SMS are throttled to not notify more than once an hour for the same office. To clear the
  throttle: `rm -rf sms-sent`.
* Headless Firefox is used via selenium. You need to have Firefox installed.
