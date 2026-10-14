package com.bookworm.shipping.mapper;

import com.bookworm.shipping.dto.ShipmentTrackingResponse;
import com.bookworm.shipping.dto.TrackingEventResponse;
import com.bookworm.shipping.entity.Shipment;
import com.bookworm.shipping.entity.TrackingEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShippingMapper {

    @Mapping(target = "status", expression = "java(shipment.getStatus() != null ? shipment.getStatus().name() : null)")
    @Mapping(target = "events", source = "trackingEvents")
    ShipmentTrackingResponse toTrackingResponse(Shipment shipment);

    @Mapping(target = "timestamp", source = "eventTimestamp")
    TrackingEventResponse toEventResponse(TrackingEvent event);

    List<TrackingEventResponse> toEventResponseList(List<TrackingEvent> events);
}
