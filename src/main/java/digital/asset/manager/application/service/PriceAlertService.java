package digital.asset.manager.application.service;

import digital.asset.manager.application.dto.PriceAlertRequest;

public interface PriceAlertService {

    void addAlert(PriceAlertRequest request);

    void removeAlert(String name);
}
