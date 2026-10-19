package com.uce.programacions.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoDto {
    private Integer id;
    private Integer userId;
    private String title;
    private Boolean completed;
    private String userName;
}
