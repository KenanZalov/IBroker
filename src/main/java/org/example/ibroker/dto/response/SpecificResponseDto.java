package org.example.ibroker.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SpecificResponseDto {

    private Long id;

    private String specificUrl;

    private Integer currentPrice;

}