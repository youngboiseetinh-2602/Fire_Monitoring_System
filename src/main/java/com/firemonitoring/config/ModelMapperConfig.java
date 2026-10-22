package com.firemonitoring.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.response.DataResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        // DataRequest is a record, so its accessors do not follow JavaBean getter names.
        mapper.createTypeMap(DataRequest.class, DataResponse.class,
                mapper.getConfiguration().copy().setFieldMatchingEnabled(true)
                        .setFieldAccessLevel(AccessLevel.PRIVATE));
        return mapper;
    }
}
