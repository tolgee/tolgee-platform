package com.polygloat.dtos.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AbstractRepositoryDTO {
    @Getter
    @Setter
    @NotNull
    @Size(min = 3, max = 500)
    String name;
}
