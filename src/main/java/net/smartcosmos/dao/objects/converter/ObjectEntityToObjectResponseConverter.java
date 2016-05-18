package net.smartcosmos.dao.objects.converter;

import net.smartcosmos.dao.objects.domain.ObjectEntity;
import net.smartcosmos.dto.objects.ObjectResponse;
import net.smartcosmos.util.UuidUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.stereotype.Component;

/**
 * @author voor
 */
@Component
public class ObjectEntityToObjectResponseConverter
        implements Converter<ObjectEntity, ObjectResponse>, FormatterRegistrar {

    @Override
    public ObjectResponse convert(ObjectEntity entity) {

        if (entity == null) {
            return null;
        }

        return ObjectResponse.builder()
                // Required
                .urn(UuidUtil.getUrnFromUuid(entity.getId()))
                .objectUrn(entity.getObjectUrn())
                .accountUrn(UuidUtil.getAccountUrnFromUuid(entity.getAccountId()))
                .type(entity.getType()).name(entity.getName())
                .lastModifiedTimestamp(entity.getLastModified() != null ? entity.getLastModified() : entity.getCreated())
                .activeFlag(entity.getActiveFlag())
                // Optional
                .moniker(entity.getMoniker())
                .description(entity.getDescription())
                // Don't forget to build it!
                .build();
    }

    @Override
    public void registerFormatters(FormatterRegistry registry) {
        registry.addConverter(this);
    }
}
