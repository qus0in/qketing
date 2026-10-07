-- queue admission: transition-only atomic state change + PUBLISH (T47-22).
-- KEYS: active, waiting, sessionKey
-- ARGV: memberId, capacity, ttlMillis, performanceId, channel
-- return 1 = ACTIVE, 0 = WAITING. Repeated ACTIVE / duplicate WAITING enqueue publish nothing.
-- active ZSET score는 first-admission 만료 시각이며, capacity 판정 전에 PTTL<=0 session을 정리한다.
local activeKey = KEYS[1]
local waitingKey = KEYS[2]
local sessionKey = KEYS[3]
local memberId = ARGV[1]
local capacity = tonumber(ARGV[2])
local ttlMillis = tonumber(ARGV[3])
local performanceId = tonumber(ARGV[4])
local channel = ARGV[5]
local serverTime = redis.call('TIME')
local nowMillis = (serverTime[1] * 1000) + math.floor(serverTime[2] / 1000)

local function publish(status)
    redis.call('PUBLISH', channel, cjson.encode({
        performanceId = performanceId,
        memberId = memberId,
        status = status,
        occurredAt = nowMillis
    }))
end

local function reconcileActive()
    for _, member in ipairs(redis.call('ZRANGE', activeKey, 0, -1)) do
        local score = redis.call('ZSCORE', activeKey, member)
        if tonumber(score) <= nowMillis
                or redis.call('EXISTS', activeKey .. ':' .. member) == 0 then
            redis.call('ZREM', activeKey, member)
        end
    end
end

redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', nowMillis)
if redis.call('ZSCORE', activeKey, memberId) then
    if redis.call('PTTL', sessionKey) > 0 then
        redis.call('ZREM', waitingKey, memberId)
        return 1
    end
    redis.call('ZREM', activeKey, memberId)
end

if redis.call('ZCARD', activeKey) >= capacity then
    reconcileActive()
end

if redis.call('ZCARD', activeKey) < capacity then
    redis.call('SET', sessionKey, 'active', 'PX', ttlMillis)
    redis.call('ZADD', activeKey, nowMillis + ttlMillis, memberId)
    redis.call('ZREM', waitingKey, memberId)
    publish('ACTIVE')
    return 1
end

redis.call('ZREMRANGEBYSCORE', waitingKey, '-inf', nowMillis - ttlMillis)
local added = redis.call('ZADD', waitingKey, 'NX', nowMillis, memberId)
redis.call('PEXPIRE', waitingKey, ttlMillis)
if added == 1 then
    publish('WAITING')
end
return 0
