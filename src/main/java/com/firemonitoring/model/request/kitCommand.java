package com.firemonitoring.model.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class kitCommand {
    private boolean buzz;
    private boolean pump;
    private boolean led;
}
