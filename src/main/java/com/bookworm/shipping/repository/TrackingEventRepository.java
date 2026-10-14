package com.bookworm.shipping.repository;

import com.bookworm.shipping.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {

    List<TrackingEvent> findByShipmentShipmentIdOrderByEventTimestampDesc(UUID shipmentId);
}
