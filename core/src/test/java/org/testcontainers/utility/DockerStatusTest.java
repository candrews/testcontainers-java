package org.testcontainers.utility;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(Parameterized.class)
public class DockerStatusTest {

    private static Instant now = Instant.now();

    @Parameterized.Parameter(0)
    public InspectContainerResponse.ContainerState running;
    @Parameterized.Parameter(1)
    public InspectContainerResponse.ContainerState runningVariant;
    @Parameterized.Parameter(2)
    public InspectContainerResponse.ContainerState shortRunning;
    @Parameterized.Parameter(3)
    public InspectContainerResponse.ContainerState created;
    @Parameterized.Parameter(4)
    public InspectContainerResponse.ContainerState createdVariant;
    @Parameterized.Parameter(5)
    public InspectContainerResponse.ContainerState exited;
    @Parameterized.Parameter(6)
    public InspectContainerResponse.ContainerState paused;
    @Parameterized.Parameter(7)
    public String name;

    private static Duration minimumDuration = Duration.ofMillis(20);

    @Test
    public void testRunning() throws Exception {
        assertTrue(DockerStatus.isContainerRunning(running, minimumDuration, now));
        assertTrue(DockerStatus.isContainerRunning(runningVariant, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(shortRunning, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(created, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(createdVariant, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(exited, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(paused, minimumDuration, now));
    }

    @Test
    public void testStopped() throws Exception {
        assertFalse(DockerStatus.isContainerStopped(running));
        assertFalse(DockerStatus.isContainerStopped(runningVariant));
        assertFalse(DockerStatus.isContainerStopped(shortRunning));
        assertFalse(DockerStatus.isContainerStopped(created));
        assertFalse(DockerStatus.isContainerStopped(createdVariant));
        assertTrue(DockerStatus.isContainerStopped(exited));
        assertFalse(DockerStatus.isContainerStopped(paused));
    }

    // ContainerState is a non-static inner class, with private member variables, in a different package.
    // It's simpler to mock it that to try to create one.
    private static InspectContainerResponse.ContainerState buildState(boolean running, boolean paused,
                                                                      String startedAt, String finishedAt) {

        InspectContainerResponse.ContainerState state = Mockito.mock(InspectContainerResponse.ContainerState.class);
        when(state.getRunning()).thenReturn(running);
        when(state.getPaused()).thenReturn(paused);
        when(state.getStartedAt()).thenReturn(startedAt);
        when(state.getFinishedAt()).thenReturn(finishedAt);
        return state;
    }

    private static Object[] createTestData(String name, Function<Instant, String> formatter) {
        return new Object[]{
            buildState(true, false, formatter.apply(now.minusMillis(30)), DockerStatus.DOCKER_TIMESTAMP_ZERO),
            buildState(true, false, formatter.apply(now.minusMillis(30)), ""),
            buildState(true, false, formatter.apply(now.minusMillis(10)), DockerStatus.DOCKER_TIMESTAMP_ZERO),
            buildState(false, false, DockerStatus.DOCKER_TIMESTAMP_ZERO, DockerStatus.DOCKER_TIMESTAMP_ZERO),
            // a container in the "created" state is not running, and has both startedAt and finishedAt empty.
            buildState(false, false, null, null),
            buildState(false, false, formatter.apply(now.minusMillis(100)), formatter.apply(now.minusMillis(90))),
            buildState(false, true, formatter.apply(now.minusMillis(100)), DockerStatus.DOCKER_TIMESTAMP_ZERO),
            name
        };
    }

    @Parameterized.Parameters(name = "{7}")
    public static Object[][] data() {
        return new Object[][]{
            createTestData("with UTC",
                DateTimeFormatter.ISO_INSTANT::format),
            createTestData("with positive offset",
                inst -> inst.atOffset(ZoneOffset.ofHours(1)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
            createTestData("with negative offset",
                inst -> inst.atOffset(ZoneOffset.ofHoursMinutes(-1, -30)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
        };
    }
}
