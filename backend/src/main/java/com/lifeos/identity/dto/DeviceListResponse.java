package com.lifeos.identity.dto;

import java.util.List;

/** GET /devices: { "items": [...] }, không phân trang (số thiết bị nhỏ, API_CONVENTIONS §8.3). */
public record DeviceListResponse(List<DeviceResponse> items) {
}
