package com.campusrelay.domain.notify.ports;

/**
 * 站内消息仓储。资金事件消费端的落点，缓存与信用版本 接 WebSocket 时直接读这张表。
 */
public interface NotificationRepository {

    /**
     * 幂等插入：msgKey + userId 唯一。
     * MQ 至少一次投递，消费端必须自己保证重复消费无副作用。
     *
     * @return true 表示新插入，false 表示已存在（重复消费）
     */
    boolean insertIfAbsent(long id, String msgKey, long userId, long errandId,
                           String type, String content);
}
