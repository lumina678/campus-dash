package com.campusrelay.infrastructure.grab;

import com.campusrelay.domain.grab.ports.GrabRateLimiterPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * 性能调优版本 本地热点限流实现。
 *
 * 口径是"单实例、按 errandId、每秒最多 N 个抢单请求"。它不替代生产级 Sentinel，
 * 但能先把 L1 限流语义接进业务链路，并为后续替换 Sentinel 热点参数限流保留端口。
 */
@Component
public class LocalGrabRateLimiterAdapter implements GrabRateLimiterPort {

    private final boolean enabled;
    private final int permitsPerSecond;
    private final LongSupplier currentTimeMillis;
    private final Map<Long, WindowCounter> counters = new ConcurrentHashMap<>();

    @Autowired
    public LocalGrabRateLimiterAdapter(
            @Value("${relay.grab.limit.enabled:true}") boolean enabled,
            @Value("${relay.grab.limit.per-second:500}") int permitsPerSecond) {
        this(enabled, permitsPerSecond, System::currentTimeMillis);
    }

    LocalGrabRateLimiterAdapter(boolean enabled, int permitsPerSecond, LongSupplier currentTimeMillis) {
        this.enabled = enabled;
        this.permitsPerSecond = Math.max(1, permitsPerSecond);
        this.currentTimeMillis = currentTimeMillis;
    }

    @Override
    public boolean tryPass(long errandId, long runnerId) {
        if (!enabled) {
            return true;
        }
        long currentSecond = currentTimeMillis.getAsLong() / 1000;
        WindowCounter counter = counters.compute(errandId, (id, old) -> {
            if (old == null || old.second != currentSecond) {
                return new WindowCounter(currentSecond);
            }
            return old;
        });
        boolean passed = counter.count.incrementAndGet() <= permitsPerSecond;
        if ((currentSecond & 0x3f) == 0) {
            prune(currentSecond);
        }
        return passed;
    }

    private void prune(long currentSecond) {
        Iterator<Map.Entry<Long, WindowCounter>> iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            if (currentSecond - iterator.next().getValue().second > 60) {
                iterator.remove();
            }
        }
    }

    private static final class WindowCounter {
        private final long second;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long second) {
            this.second = second;
        }
    }
}
