local activeKey = KEYS[1]
local waitingKey = KEYS[2]
local sessionKey = KEYS[3]
local memberId = ARGV[1]
local capacity = tonumber(ARGV[2])
local ttlMillis = tonumber(ARGV[3])
local serverTime = redis.call('TIME')
local nowMillis = (serverTime[1] * 1000) + math.floor(serverTime[2] / 1000)
local expiresAt = nowMillis + ttlMillis

redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', nowMillis)
if redis.call('ZSCORE', activeKey, memberId) then
    redis.call('ZREM', waitingKey, memberId)
    return 1
end

if redis.call('ZCARD', activeKey) < capacity then
    redis.call('ZADD', activeKey, expiresAt, memberId)
    redis.call('SET', sessionKey, 'active', 'PX', ttlMillis)
    redis.call('ZREM', waitingKey, memberId)
    return 1
end

redis.call('ZADD', waitingKey, 'NX', nowMillis, memberId)
return 0
