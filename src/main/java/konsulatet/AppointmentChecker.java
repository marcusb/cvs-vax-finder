package konsulatet;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppointmentChecker implements AutoCloseable {

  // edit as appropriate
  private static final String URL = "RESCHEDULE_URL_HERE";
  private static final String LOCATION = "Arlington, MA";
  private static final LocalDate LATEST_DATE = LocalDate.of(2021, 11, 12);
  private static final Map<String, String> COOKIES =
      Map.of(
          "DG_HID",
          "replace me!",
          "DG_IID",
          "replace me!",
          "DG_SID",
          "replace me!",
          "DG_UID",
          "replace me!",
          "DG_ZID",
          "replace me!",
          "DG_ZUID",
          "replace me!");

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final By CONTINUE_BTN = By.className("btn-control");

  private final WebDriver driver;

  AppointmentChecker() {
    this(createDriver());
  }

  static WebDriver createDriver() {
    WebDriverManager.firefoxdriver().setup();
    FirefoxOptions options = new FirefoxOptions(); // .setHeadless(true);
    return new FirefoxDriver(options);
  }

  public AppointmentChecker(WebDriver driver) {
    this.driver = driver;
  }

  @Override
  public void close() {
    driver.quit();
  }

  void checkappointments(BiConsumer<String, String> notifier) {

    driver.manage().deleteAllCookies();
    driver.get("https://www.cvs.com");
    COOKIES.entrySet().stream()
        .map(
            e ->
                new Cookie.Builder(e.getKey(), e.getValue())
                    .domain(".cvs.com")
                    .path("/")
                    .isHttpOnly(true)
                    .build())
        .forEach(driver.manage()::addCookie);

    var wait =
        new WebDriverWait(driver, 30, 10)
            .ignoring(StaleElementReferenceException.class)
            .ignoring(ElementClickInterceptedException.class)
            .ignoring(NoSuchElementException.class);

    log.debug("opening url: {}", URL);
    driver.get(URL);

    wait.until(elementToBeClickable(By.xpath(".//section[contains(@class, 'dose-container')]//a")))
        .click();
    driver.findElement(CONTINUE_BTN).click();

    log.debug("entering address");
    var loc = wait.until(visibilityOfElementLocated(By.id("address")));
    loc.sendKeys(LOCATION);

    for (; ; ) {
      var wait5 = new WebDriverWait(driver, 5, 3000);
      try {
        wait.until(
            d -> {
              loc.sendKeys(Keys.ENTER);
              var dt =
                  wait5.until(
                      visibilityOfElementLocated(
                          By.xpath(".//select[@id='availableDate']/option[1]")));
              return true;
            });
      } catch (TimeoutException ex) {
        continue;
      }
      var date = driver.findElement(By.xpath(".//select[@id='availableDate']/option[1]")).getText();
      var storeName = driver.findElement(By.className("store-name")).getText();
      var storeLoc = driver.findElement(By.className("store-details")).getText();
      var dt =
          LocalDate.parse(
              date.split(":")[1], DateTimeFormatter.ofPattern(" LLLL d, yyyy", Locale.US));
      if (dt.isAfter(LATEST_DATE)) {
        log.info("only later appointments available");
        continue;
      }

      log.info("maybe available slots on {} in {}, {}", dt, storeName, storeLoc);
      wait5.until(elementToBeClickable(By.id("availableTimes0"))).click();
      final WebElement slotBtn;
      try {
        slotBtn = wait5.until(visibilityOfElementLocated(By.className("slot-btn")));
      } catch (TimeoutException e) {
        if (driver.findElement(By.className("error-desc")).getText().contains("Walk-ins")) {
          log.info("nope, no slots available");
          continue;
        }
        throw e;
      }
      slotBtn.click();
      notifier.accept(
          String.format("%s %s", date, slotBtn.getText()),
          String.format("%s, %s", storeName, storeLoc));
      break;
    }
  }
}
