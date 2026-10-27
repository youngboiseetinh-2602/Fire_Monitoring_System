package com.firemonitoring.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.response.DataResponse;
import com.firemonitoring.model.request.AddKitRequest;
import com.firemonitoring.entity.Kit;
import com.firemonitoring.entity.Location;
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
        mapper.addConverter(context -> {
            Location location = new Location();
            location.setLocationName(context.getSource().location());
            return location;
        }, AddKitRequest.class, Location.class);
        mapper.addConverter(context -> {
            Kit kit = new Kit();
            kit.setDeviceUid(context.getSource().deviceUid());
            return kit;
        }, AddKitRequest.class, Kit.class);
        return mapper;
    }
}
