package com.bookworm.shipping.service;

import com.bookworm.shipping.dto.EDDResponse;
import com.bookworm.shipping.dto.ShipmentTrackingResponse;
import com.bookworm.shipping.entity.Shipment;

import java.util.UUID;

public interface ShippingService {

    EDDResponse calculateEdd(String pincode);

    ShipmentTrackingResponse trackShipmentByTrackingNumber(String trackingNumber);

    ShipmentTrackingResponse trackShipmentByOrderId(UUID orderId);

    Shipment createShipment(UUID orderId, String carrierName);
}
