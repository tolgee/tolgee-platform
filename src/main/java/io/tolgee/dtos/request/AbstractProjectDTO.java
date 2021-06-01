package io.tolgee.dtos.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AbstractProjectDTO {
    @NotNull
    @Size(min = 3, max = 500)
    String name;

    public @NotNull @Size(min = 3, max = 500) String getName() {
        return this.name;
    }

    public void setName(@NotNull @Size(min = 3, max = 500) String name) {
        this.name = name;
    }
}
