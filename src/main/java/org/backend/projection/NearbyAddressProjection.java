package org.backend.projection;

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
    Double getDistanceKm();
}