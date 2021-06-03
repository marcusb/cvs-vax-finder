package konsulatet;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Consumer;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppointmentChecker implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final By MAKE_RESERVATION = By.id("cl_Dj-9");
  private static final By NEXT_BTN = By.id("action_7");
  private static final By CANCEL_RESERVATION_RADIOBTN =
      By.xpath(".//input[contains(@class, 'FastComboButtonItem_CANCEL')]");
  private static final By TXN_TYPE_SEL = By.tagName("select").className("DFI");
  private static final By SELECT_DATE_LINK = By.className("IconCaptionText");
  private static final By LOCATION_SEL = By.tagName("select").id("Dc_1-9");
  private static final By DATE_CELL = By.className("TDC");
  private static final By NOT_YET_AVAILABLE =
      By.xpath(
          ".//td[contains(@class, 'TDC')][contains(@class, ' Field ')][contains(@style, 'F1F1F1')]");
  private static final By UNAVAILABLE_DATES =
      By.xpath(
          ".//td[contains(@class, 'TDC')][contains(@class, ' Field ')][contains(@style, '746d40')]");
  private static final By AVAILABLE_DATES =
      By.xpath(
          ".//td[contains(@class, 'TDC')][contains(@class, ' Field ')][contains(@style, 'CBFFCC')]");
  private static final By NEXT_MONTH = By.id("cl_Dc_1-r");

  private static final String URL = "https://atlas-myrmv.massdot.state.ma.us/myrmv/_/";

  // comment out as appropriate
  private static final List<String> OFFICES =
      List.of(
          "Brockton RMV",
          "Danvers RMV",
          "Fall River RMV",
          "Haymarket RMV",
          "Lawrence RMV",
          "Leominster RMV",
          "Martha's Vineyard RMV",
          "Nantucket RMV",
          "New Bedford RMV",
          "North Adams RMV",
          "Pittsfield RMV",
          "Plymouth RMV",
          "Revere RMV",
          "South Yarmouth RMV",
          "Springfield RMV",
          "Watertown RMV",
          "Worcester RMV");

  private final WebDriver driver;

  AppointmentChecker() {
    this(createDriver());
  }

  static WebDriver createDriver() {
    WebDriverManager.firefoxdriver().setup();
    FirefoxOptions options = new FirefoxOptions().setHeadless(true);
    return new FirefoxDriver(options);
  }

  public AppointmentChecker(WebDriver driver) {
    this.driver = driver;
  }

  @Override
  public void close() {
    driver.quit();
  }

  void checkappointments(Consumer<String> notifier) {
    driver.manage().deleteAllCookies();

    var wait =
        new WebDriverWait(driver, 300, 10)
            .ignoring(StaleElementReferenceException.class)
            .ignoring(ElementClickInterceptedException.class)
            .ignoring(NoSuchElementException.class);

    log.debug("opening url: {}", URL);
    driver.get(URL);

    log.debug("waiting for main menu");
    var mkReservationBtn = wait.until(elementToBeClickable(MAKE_RESERVATION));
    wait.until(
        d -> {
          log.debug("clicking Make Reservation");
          mkReservationBtn.click();
          return true;
        });

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      log.debug("sleep interrupted");
    }
    log.debug("looking for Next button");
    var next = wait.until(visibilityOfElementLocated(NEXT_BTN));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    log.debug("clicking Next button");
    next.click();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    log.debug("waiting for Make Reservation button");
    var makeResBtn = wait.until(visibilityOfElementLocated(CANCEL_RESERVATION_RADIOBTN));
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    log.debug("selecting Make Reservation");
    makeResBtn.sendKeys(Keys.LEFT);

    log.debug("waiting for transaction type selector");
    var txnTypeSel = wait.until(visibilityOfElementLocated(TXN_TYPE_SEL));
    var selectTxnType = new Select(txnTypeSel);
    log.debug("selecting Apply for Learner's permit");
    selectTxnType.selectByValue("TXN01");

    log.debug("clicking Select Date");
    var selectDate = driver.findElement(SELECT_DATE_LINK);
    selectDate.click();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (var office : OFFICES) {
      log.debug("checking {}", office);
      wait.until(
          d -> {
            var locSel = driver.findElement(LOCATION_SEL);
            var locSelect = new Select(locSel);
            locSelect.selectByVisibleText(office);
            return true;
          });

      log.debug("waiting for date picker");
      wait.until(visibilityOfElementLocated(DATE_CELL));

      if (hasAvailability()) {
        notifier.accept(office);
      }

      log.debug("clicking next month");
      var nextMonth = wait.until(visibilityOfElementLocated(NEXT_MONTH));
      nextMonth.click();
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (hasAvailability()) {
        notifier.accept(office);
      }
    }
  }

  private boolean hasAvailability() {
    var unavailable = driver.findElements(UNAVAILABLE_DATES);
    var available = driver.findElements(AVAILABLE_DATES);
    var notYetAvailable = driver.findElements(NOT_YET_AVAILABLE);
    log.debug(
        "available={} unavailable={} not-yet={}",
        available.size(),
        unavailable.size(),
        notYetAvailable.size());
    return !available.isEmpty();
  }
}
