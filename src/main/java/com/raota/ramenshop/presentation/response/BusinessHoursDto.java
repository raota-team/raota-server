package com.raota.ramenshop.presentation.response;

import com.raota.ramenshop.domain.model.BusinessHours;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BusinessHoursDto {
    private String closed_days;
    private LocalTime open_time;
    private LocalTime close_time;
    private LocalTime break_start;
    private LocalTime break_end;
    private String parking_info;

    public static BusinessHoursDto from(BusinessHours businessHours){
        if (businessHours == null) {
            return null;
        }
        return new BusinessHoursDto(
                businessHours.closedDays(),
                businessHours.openTime(),
                businessHours.closeTime(),
                businessHours.breakStart(),
                businessHours.breakEnd(),
                businessHours.parkingInfo()
        );
    }
}
