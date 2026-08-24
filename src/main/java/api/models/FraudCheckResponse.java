package api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FraudCheckResponse extends BaseModel {
    private String transactionId;
    private String status;
    private String message;
    private String fraudCheckStatus;
    private String details;
}
