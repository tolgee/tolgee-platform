package io.polygloat.exceptions;

import io.polygloat.constants.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(value = HttpStatus.NOT_FOUND)
@NoArgsConstructor
@Data
public class NotFoundException extends RuntimeException {
    private Message msg = Message.RESOURCE_NOT_FOUND;

    public NotFoundException(Message msg){
        this.msg = msg;
    }
}
