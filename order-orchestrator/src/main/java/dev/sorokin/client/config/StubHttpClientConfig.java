package dev.sorokin.client.config;

import dev.sorokin.client.StubHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class StubHttpClientConfig {

    @Bean
    public ClientHttpRequestFactory jdkRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) //поменять потом
                .build();
        return new JdkClientHttpRequestFactory(httpClient);
    }

    @Bean
    public StubHttpClient httpClient(
            RestClient.Builder builder,
            StubHttpClientProperties properties,
            ClientHttpRequestFactory jdkRequestFactory
    ){
        RestClient restClient = builder
                .baseUrl(properties.getPaymentUrl())
                .requestFactory(jdkRequestFactory)
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(StubHttpClient.class);
    }
}
