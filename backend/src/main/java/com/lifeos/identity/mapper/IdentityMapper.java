package com.lifeos.identity.mapper;

import com.lifeos.identity.dto.DeviceResponse;
import com.lifeos.identity.dto.ProfileResponse;
import com.lifeos.identity.dto.SettingsResponse;
import com.lifeos.identity.dto.UserSummary;
import com.lifeos.identity.entity.Device;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.entity.UserSettings;
import org.mapstruct.Mapper;

/**
 * Mapper entity → DTO của module identity (D5: MapStruct, unmappedTargetPolicy = ERROR cấu hình toàn cục trong
 * pom.xml). Không chứa business logic, không gọi repository/service.
 */
@Mapper
public interface IdentityMapper {

    UserSummary toSummary(User user);

    /** emailVerified lấy từ User.isEmailVerified(). */
    ProfileResponse toProfile(User user);

    @org.mapstruct.Mapping(target = "quietStart", dateFormat = "HH:mm")
    @org.mapstruct.Mapping(target = "quietEnd", dateFormat = "HH:mm")
    SettingsResponse toSettings(UserSettings settings);

    /** current lấy từ tham số cùng tên (thiết bị của access token đang dùng). */
    DeviceResponse toResponse(Device device, boolean current, String pushTokenSuffix);
}
