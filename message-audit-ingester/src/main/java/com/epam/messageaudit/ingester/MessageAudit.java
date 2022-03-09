package com.epam.messageaudit.ingester;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.Data;

@Data
public class MessageAudit {

    private String traceID;
    private String status;
    private String productName;
    private String sourceSystem;
    private String targetSystem;
    private List<TargetState> targetState = new ArrayList();
    private String productId;
    private String error;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date startTimestamp;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date updateTimestamp;

    public TargetState getBySystem(String system) {
        TargetState result;
        final Optional<TargetState> target = targetState.stream()
            .filter(ts -> ts.getSystem().equalsIgnoreCase(system)).findAny();
        if (!target.isPresent()) {
            TargetState newTargetState = new TargetState();
            newTargetState.setSystem(system);
            targetState.add(newTargetState);
            result = newTargetState;
        } else {
            result = target.get();
        }
        return result;
    }

}
