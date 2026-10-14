package com.bookworm.shipping.service;

import com.bookworm.common.exception.BadRequestException;
import com.bookworm.common.exception.ResourceNotFoundException;
import com.bookworm.order.entity.Order;
import com.bookworm.order.entity.OrderStatus;
import com.bookworm.order.entity.OrderStatusHistory;
import com.bookworm.order.repository.OrderRepository;
import com.bookworm.shipping.dto.EDDResponse;
import com.bookworm.shipping.dto.ShipmentTrackingResponse;
import com.bookworm.shipping.entity.Shipment;
import com.bookworm.shipping.entity.ShipmentStatus;
import com.bookworm.shipping.entity.TrackingEvent;
import com.bookworm.shipping.mapper.ShippingMapper;
import com.bookworm.shipping.repository.ShipmentRepository;
import com.bookworm.shipping.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final OrderRepository orderRepository;
    private final ShippingMapper shippingMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public EDDResponse calculateEdd(String pincode) {
        log.info("Calculating Estimated Delivery Date (EDD) for PIN code: {}", pincode);

        if (pincode == null || !pincode.matches("^[1-9][0-9]{5}$")) {
            log.warn("Invalid Indian postal PIN code format: {}", pincode);
            return EDDResponse.builder()
                    .pinCode(pincode)
                    .tentativeDeliveryDate(null)
                    .deliveryDays(0)
                    .isServiceable(false)
                    .build();
        }

        // Metro PIN codes (starting with 11, 40, 56, 60, 70) have 2-day delivery; others 3-4 days
        int deliveryDays = 3;
        if (pincode.startsWith("11") || pincode.startsWith("40") ||
            pincode.startsWith("56") || pincode.startsWith("60") || pincode.startsWith("70")) {
            deliveryDays = 2;
        }

        LocalDate edd = LocalDate.now().plusDays(deliveryDays);

        return EDDResponse.builder()
                .pinCode(pincode)
                .tentativeDeliveryDate(edd)
                .deliveryDays(deliveryDays)
                .isServiceable(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentTrackingResponse trackShipmentByTrackingNumber(String trackingNumber) {
        log.info("Tracking shipment with tracking number: {}", trackingNumber);
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "trackingNumber", trackingNumber));
        return shippingMapper.toTrackingResponse(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentTrackingResponse trackShipmentByOrderId(UUID orderId) {
        log.info("Tracking shipment for order ID: {}", orderId);
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "orderId", orderId));
        return shippingMapper.toTrackingResponse(shipment);
    }

    @Override
    @Transactional
    public Shipment createShipment(UUID orderId, String carrierName) {
        log.info("Creating shipment for order: {} with carrier: {}", orderId, carrierName);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (shipmentRepository.findByOrderOrderId(orderId).isPresent()) {
            throw new BadRequestException("Shipment already exists for order: " + orderId);
        }

        String trackingNumber = "TRK-" + System.currentTimeMillis() + "-" + (1000 + secureRandom.nextInt(9000));
        LocalDate edd = order.getTentativeDeliveryDate() != null ? order.getTentativeDeliveryDate() : LocalDate.now().plusDays(3);

        Shipment shipment = Shipment.builder()
                .order(order)
                .carrierName(carrierName != null ? carrierName : "BlueDart Express")
                .trackingNumber(trackingNumber)
                .status(ShipmentStatus.LABEL_CREATED)
                .estimatedDeliveryDate(edd)
                .build();

        Shipment savedShipment = shipmentRepository.save(shipment);

        TrackingEvent initialEvent = TrackingEvent.builder()
                .shipment(savedShipment)
                .eventStatus("LABEL_CREATED")
                .location("Central Fulfillment Center, Mumbai")
                .eventTimestamp(Instant.now())
                .description("Shipping label created and assigned to carrier " + savedShipment.getCarrierName())
                .build();
        trackingEventRepository.save(initialEvent);

        savedShipment.addTrackingEvent(initialEvent);

        // Update order status to PROCESSING
        order.setStatus(OrderStatus.PROCESSING);
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PROCESSING)
                .notes("Shipment created with tracking number: " + trackingNumber)
                .build();
        order.addStatusHistory(history);
        orderRepository.save(order);

        log.info("Created shipment {} with tracking number {}", savedShipment.getShipmentId(), trackingNumber);
        return savedShipment;
    }
}
