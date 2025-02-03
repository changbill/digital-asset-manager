package digital.asset.manager.application.chart.controller;

import digital.asset.manager.application.chart.dto.PriceAlertRequest;
import digital.asset.manager.application.chart.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Slf4j
public class PriceAlertController {

    private final PriceAlertService alertService;

    @PostMapping
    public ResponseEntity<String> createAlert(@RequestBody PriceAlertRequest request) {
        alertService.addAlert(request);
        return ResponseEntity.ok("Alert added successfully!");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteAlert(@PathVariable String name) {
        alertService.removeAlert(name);
        return ResponseEntity.ok("Alert removed successfully!");
    }
}
