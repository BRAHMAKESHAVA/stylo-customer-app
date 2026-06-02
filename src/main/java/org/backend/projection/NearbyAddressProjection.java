package org.backend.projection;

import org.backend.enums.AddressType;

import java.math.BigDecimal;

public interface NearbyAddressProjection {

    Long getAddressId();
    Long getCustomerId();
    String getCustomerName();
    String getHouseNumber();
    String getBuildingName();
    String getArea();
    String getLandmark();
    String getCity();
    String getState();
    String getPinCode();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    AddressType getAddressType();
    String getLabelName();
    Boolean getIsDefault();
    Boolean getIsSelected();
    Double getDistanceKm();
}