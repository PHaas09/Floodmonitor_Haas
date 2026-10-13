package com.example.floodmonitor.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

/**
 * XML-Ausgabe mit Deklaration <?xml version='1.0' encoding='UTF-8'?> am Anfang.
 * (XML funktioniert auch ohne diese Klasse, weil jackson-dataformat-xml eingebunden ist.)
 */
@Configuration
public class WebConfig {

    @Bean
    public MappingJackson2XmlHttpMessageConverter xmlConverter(Jackson2ObjectMapperBuilder builder) {
        XmlMapper xmlMapper = builder.createXmlMapper(true).build();
        xmlMapper.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);
        return new MappingJackson2XmlHttpMessageConverter(xmlMapper);
    }
}
