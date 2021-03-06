package net.smartcosmos.dao.things.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import net.smartcosmos.dao.things.domain.ThingEntity;
import net.smartcosmos.dto.things.Page;
import net.smartcosmos.dto.things.PageInformation;
import net.smartcosmos.dto.things.ThingResponse;

@Component
public class SpringDataPageToThingResponsePageConverter
    extends ConversionServiceAwareConverter<org.springframework.data.domain.Page<ThingEntity>, Page<ThingResponse>> {

    @Autowired
    private ConversionService conversionService;

    protected ConversionService conversionService() {

        return conversionService;
    }

    @Override
    public Page<ThingResponse> convert(org.springframework.data.domain.Page<ThingEntity> page) {

        PageInformation pageInformation = PageInformation.builder()
            .number((page.getTotalElements() > 0 ? page.getNumber() + 1 : 0))
            .totalElements(page.getTotalElements())
            .size(page.getNumberOfElements())
            .totalPages((page.getNumberOfElements() > 0 ? page.getTotalPages() : 0))
            .build();

        List<ThingResponse> data = page.getContent()
            .stream()
            .map(entity -> conversionService.convert(entity, ThingResponse.class))
            .collect(Collectors.toList());

        return Page.<ThingResponse>builder()
            .data(data)
            .page(pageInformation)
            .build();
    }
}
