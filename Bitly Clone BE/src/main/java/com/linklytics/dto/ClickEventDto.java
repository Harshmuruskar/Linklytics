package com.linklytics.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ClickEventDto {
    LocalDate clickDate;
    Long count;
}
