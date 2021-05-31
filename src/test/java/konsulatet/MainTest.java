package konsulatet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MainTest {

  private static final Map<String, String> OFFICES =
      Map.of("foo", "http://foo.example.com/", "bar", "http://bar.example.com/");

  @Mock SmsSender smsSender;
  @Mock AppointmentChecker appointmentChecker;

  private Main main;

  private CompletableFuture<Void> f;

  @Before
  public void setUp() {
    main = new Main(smsSender, appointmentChecker);
  }

  @After
  public void tearDown() throws Exception {
    main.close();
    if (f != null) {
      f.cancel(true);
      try {
        f.get(30, TimeUnit.SECONDS);
      } catch (CancellationException | InterruptedException | ExecutionException e) {
        // ignore
      }
    }
  }

  @Test
  public void shouldSendSmsWhenApptFound() {
    Mockito.doAnswer(
            inv -> {
              Consumer<String> notifier = inv.getArgument(0);
              notifier.accept("Watertown RMV");
              return null;
            })
        .when(appointmentChecker)
        .checkappointments(any(Consumer.class));

    f = CompletableFuture.runAsync(() -> main.check());
    verify(smsSender, timeout(30_000).atLeastOnce())
        .sendSMS("Watertown RMV", "Appointments might be available at Watertown RMV");
  }

  @Test
  public void shouldSendSmsWhenCrashing() {
    Mockito.doThrow(new RuntimeException("expected exception"))
        .when(appointmentChecker)
        .checkappointments(any(Consumer.class));

    f = CompletableFuture.runAsync(() -> main.check());
    verify(smsSender, timeout(30_000).atLeastOnce()).sendCrash(anyString());
  }

  @Test
  public void shouldSendSmsWhenStarting() {
    Mockito.doAnswer(
            inv -> {
              Consumer<String> notifier = inv.getArgument(0);
              notifier.accept("Watertown RMV");
              return null;
            })
        .when(appointmentChecker)
        .checkappointments(any(Consumer.class));

    f = CompletableFuture.runAsync(() -> main.check());
    verify(smsSender, timeout(30_000).atLeastOnce()).sendHello();
  }
}
