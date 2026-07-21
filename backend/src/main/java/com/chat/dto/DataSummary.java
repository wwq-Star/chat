package com.chat.dto;

import lombok.Data;

@Data
public class DataSummary {
    private boolean success;
    private Long totalCount;
    private Long userCount;
    private String minTime;
    private String maxTime;
    private String message;
}

