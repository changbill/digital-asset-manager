package digital.asset.manager.application.chart.service;

import digital.asset.manager.application.chart.dto.PriceAlertRequest;

public interface PriceAlertService {

    void addAlert(PriceAlertRequest request);

    void removeAlert(String name);
}
