package com.rosanova.iot.timer.user.dto;

import com.rosanova.iot.timer.error.Result;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.management.relation.RelationSupport;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginReturnDto {
    Result result;
    String token;
}
