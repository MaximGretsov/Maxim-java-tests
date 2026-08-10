package api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    private int id;
    private float amount;
    private String type;
    private String timestamp;
    private String timestampAsString;
    private int relatedAccountId;
    private Double amountAsDouble;
}
