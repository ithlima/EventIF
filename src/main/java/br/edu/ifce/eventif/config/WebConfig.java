package br.edu.ifce.eventif.config;

import java.math.BigDecimal;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configurações MVC para conversão numérica pt-BR/ISO em formulários (ex.: carga horária "1,5" ou "1.5").
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, BigDecimal>() {
            @Override
            public BigDecimal convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                String normalizado = source.trim().replace(',', '.');
                return new BigDecimal(normalizado);
            }
        });
    }
}
