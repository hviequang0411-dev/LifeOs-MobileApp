package com.lifeos.common.device;

import java.util.Map;
import java.util.UUID;

/** GET /devices hiện 6 ký tự cuối của token push (D147). notification cài đặt; identity chỉ biết interface này. */
public interface PushTokenLookup {

    /** @return deviceId → 6 ký tự cuối của token push (chỉ thiết bị có token) */
    Map<UUID, String> tokenSuffixes(UUID userId);
}
