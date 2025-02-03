package digital.asset.manager.application.chart.controller;

import digital.asset.manager.application.chart.service.ExternalPriceService;
import digital.asset.manager.application.chart.service.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/price")
@Slf4j
public class PriceController {
    private final ExternalPriceService externalPriceService;
    private final PriceService priceService;

    @GetMapping
    public ResponseEntity<String> getCurrentPrice() {
        // Redis에서 최신 시세 가져오기
        String price = priceService.getPrice();

        if(price == null) {
            price = externalPriceService.fetchPriceFromApi();
            if(price == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("현재 가격 데이터를 가져올 수 없습니다. 잠시 후 다시 시도하세요.");
            }
        }
        return ResponseEntity.ok("현재 BTC 가격: " + price);
    }

}
