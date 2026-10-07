-- queue 상태 원자 read: 만료 active 정리 + 만료 waiting 정리 후 ACTIVE/WAITING/ABSENT 판정.
-- 반환: {1, sessionTtlMillis} = ACTIVE, {2, rank+1} = WAITING, {0, 0} = ABSENT
-- KEYS: active, waiting, sessionKey / ARGV: memberId, ttlMillis
local activeKey = KEYS[1]
local waitingKey = KEYS[2]
local sessionKey = KEYS[3]
local memberId = ARGV[1]
local ttlMillis = tonumber(ARGV[2])
local serverTime = redis.call('TIME')
local nowMillis = (serverTime[1] * 1000) + math.floor(serverTime[2] / 1000)

redis.call('ZREMRANGEBYSCORE', activeKey, '-inf', nowMillis)
redis.call('ZREMRANGEBYSCORE', waitingKey, '-inf', nowMillis - ttlMillis)

if redis.call('ZSCORE', activeKey, memberId) then
    local ttl = redis.call('PTTL', sessionKey)
    if ttl > 0 then
        return {1, ttl}
    end
    redis.call('ZREM', activeKey, memberId)
    return {0, 0}
end

local rank = redis.call('ZRANK', waitingKey, memberId)
if rank then
    return {2, rank + 1}
end

return {0, 0}
