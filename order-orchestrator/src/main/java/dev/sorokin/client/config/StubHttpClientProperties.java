package dev.sorokin.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment") //указывает на блок "payment" в application.yaml

@Getter
@Setter
public class StubHttpClientProperties {

    //имя поля baseUrl автоматическое соответствие ключу base-url в YAML
    private String paymentUrl;

//    public String getPaymentUrl() {
//        return paymentUrl;
//    }
//
//    public void setPaymentUrl(String paymentUrl) {
//        this.paymentUrl = paymentUrl;
//    }
}
