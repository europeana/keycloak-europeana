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

     /** Tests if initial delay is correctly calculated for starting the scheduler by
      * {@code FixedRateTaskScheduler.calculateInitialDelayInMillis(int)} method
      * @param currentMinute Simulated value for Minute of hour.
      * @param currentSeconds Simulated value for Seconds of minute.
      * @param intervalMinute Simulated value for Interval in minutes.
      * @param expected Expected value for delay in milliseconds.
      */
     @ParameterizedTest
     @CsvSource({
             "0,43,10,557_000",
             "2,59,3,1000",
             "59,59,60,1000",
             "0,0,0,0",
             "10,0,10,600_000",
             "10,0,70,600_000",
             "10,23,72,97_000",
             "50,0,70,600_000",
             "0,0,60,3_600_000",
             "30,0,60,1_800_000",
             "35,0,60,1_500_000",
             "10,0,140,600_000",
             "10,0,137,420_000"
     })
     void testCalculateinitialDelayInMillis(int currentMinute,int currentSeconds,int intervalMinute,int expected){
         FixedRateTaskScheduler fixedRateTaskScheduler = Mockito.spy(new FixedRateTaskScheduler(customScheduledTask, intervalMinute));
         LocalDateTime mockTime = LocalDateTime.of(2025, Month.JANUARY, 1, 10, currentMinute,currentSeconds);
         when(fixedRateTaskScheduler.getLocalTime()).thenReturn(mockTime);
         long delay = fixedRateTaskScheduler.calculateInitialDelayInMillis(intervalMinute);
         assertEquals(expected,delay);
     }



}