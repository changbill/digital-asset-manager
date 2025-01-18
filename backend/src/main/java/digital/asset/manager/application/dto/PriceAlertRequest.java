package digital.asset.manager.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertRequest {

    private String name;
    private double highThreshold;
    private double lowThreshold;
}
