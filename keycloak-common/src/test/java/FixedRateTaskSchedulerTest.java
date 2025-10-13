import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import eu.europeana.keycloak.timer.AbstractCustomScheduledTask;
import eu.europeana.keycloak.timer.FixedRateTaskScheduler;

import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.Mock;
import org.mockito.Mockito;


 class FixedRateTaskSchedulerTest {
  @Mock
  private AbstractCustomScheduledTask customScheduledTask;
  @ParameterizedTest
  @CsvSource({
      "0,10,10",
      "2,3,1",
      "59,60,1",
      "0,0,0",
      "10,70,10",
      "10,72,2",
      "50,70,10",
      "0,60,60",
      "30,60,30",
      "35,60,25",
      "10,140,10",
      "10,137,7"
  })
  void testCalculateinitialDelay(int currentMinute,int intervalMinute,int expected){
      // e.g.  Current minute is 0, interval is 10. Expected delay is 10 - (0 % 10) = 10.
      FixedRateTaskScheduler fixedRateTaskScheduler = Mockito.spy(new FixedRateTaskScheduler(customScheduledTask, intervalMinute));
      LocalDateTime mockTime = LocalDateTime.of(2025, Month.JANUARY, 1, 10, currentMinute, 0);
      when(fixedRateTaskScheduler.getLocalTime()).thenReturn(mockTime);
      int delay = fixedRateTaskScheduler.calculateInitialDelay(intervalMinute);
      assertEquals(expected,delay);
  }

}