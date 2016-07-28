package net.smartcosmos.dao.things.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import net.smartcosmos.dao.things.SortOrder;
import net.smartcosmos.dao.things.ThingDao;
import net.smartcosmos.dao.things.domain.ThingEntity;
import net.smartcosmos.dao.things.repository.ThingRepository;
import net.smartcosmos.dao.things.util.ThingPersistenceUtil;
import net.smartcosmos.dao.things.util.UuidUtil;
import net.smartcosmos.dto.things.Page;
import net.smartcosmos.dto.things.PageInformation;
import net.smartcosmos.dto.things.ThingCreate;
import net.smartcosmos.dto.things.ThingResponse;
import net.smartcosmos.dto.things.ThingUpdate;

@Slf4j
@Service
public class ThingPersistenceService implements ThingDao {

    public static final Integer DEFAULT_PAGE = 1;
    public static final Integer DEFAULT_SIZE = 20;
    public static final Sort.Direction DEFAULT_SORT_ORDER = Sort.Direction.ASC;
    public static final String DEFAULT_SORT_BY = "created";

    private final ThingRepository repository;
    private final ConversionService conversionService;

    @Autowired
    public ThingPersistenceService(
        ThingRepository repository,
        ConversionService conversionService) {
        this.repository = repository;
        this.conversionService = conversionService;
    }

    // region Create

    @Override
    public Optional<ThingResponse> create(String tenantUrn, ThingCreate createThing) {

        if (!alreadyExists(tenantUrn, createThing)) {
            try {
                UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);

                ThingEntity entity = conversionService.convert(createThing, ThingEntity.class);
                entity.setTenantId(tenantId);

                entity = persist(entity);
                ThingResponse response = conversionService.convert(entity, ThingResponse.class);

                return Optional.ofNullable(response);
            } catch (IllegalArgumentException e) {
                if (StringUtils.isNotBlank(createThing.getUrn())) {
                    log.warn("Error processing URNs: Tenant URN '{}' - Thing URN '{}'", tenantUrn, createThing.getUrn());
                } else {
                    log.warn("Error processing ID: Tenant ID '{}'", tenantUrn);
                }
                throw e;
            }
        }

        return Optional.empty();
    }

    // endregion

    // region Update

    @Override
    public Optional<ThingResponse> update(String tenantUrn, String type, String urn, ThingUpdate updateThing) throws ConstraintViolationException {

        try {
            UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
            UUID id = UuidUtil.getUuidFromUrn(urn);
            Optional<ThingEntity> thing = repository.findByIdAndTenantIdAndTypeIgnoreCase(id, tenantId, type);

            if (thing.isPresent()) {
                ThingEntity updateEntity = ThingPersistenceUtil.merge(thing.get(), updateThing);
                updateEntity = persist(updateEntity);
                final ThingResponse response = conversionService.convert(updateEntity, ThingResponse.class);

                return Optional.ofNullable(response);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Error processing URNs: Tenant URN '{}' - Thing URN '{}'", tenantUrn, urn);
        }

        return Optional.empty();
    }

    // endregion

    // region Delete

    @Override
    public Optional<ThingResponse> delete(String tenantUrn, String type, String urn) {

        try {
            UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
            UUID id = UuidUtil.getUuidFromUrn(urn);
            List<ThingEntity> deleteList = repository.deleteByIdAndTenantIdAndTypeIgnoreCase(id, tenantId, type);

            if (!deleteList.isEmpty()) {
                return Optional.ofNullable(conversionService.convert(deleteList.get(0), ThingResponse.class));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Error processing URNs: Tenant URN '{}' - Thing URN '{}'", tenantUrn, urn);
        }

        return Optional.empty();
    }

    // endregion

    // region Find By Type

    @Override
    public Page<ThingResponse> findByType(String tenantUrn, String type) {

        return findByType(tenantUrn, type, getPageable(null, null, null, null));
    }

    @Override
    public Page<ThingResponse> findByType(String tenantUrn, String type, SortOrder sortOrder, String sortBy) {

        return findByType(tenantUrn, type, getPageable(null, null, ThingPersistenceUtil.getSortByFieldName(sortBy),
                                                       ThingPersistenceUtil.getSortDirection(sortOrder)));
    }

    @Override
    public Page<ThingResponse> findByType(String tenantUrn, String type, Integer page, Integer size) {

        return findByType(tenantUrn, type, getPageable(page, size, null, null));
    }

    @Override
    public Page<ThingResponse> findByType(String tenantUrn, String type, Integer page, Integer size, SortOrder sortOrder, String sortBy) {

        return findByType(tenantUrn, type, getPageable(page, size, ThingPersistenceUtil.getSortByFieldName(sortBy),
                                                       ThingPersistenceUtil.getSortDirection(sortOrder)));
    }

    private Page<ThingResponse> findByType(String tenantUrn, String type, Pageable pageable) {

        Page<ThingResponse> result = ThingPersistenceUtil.emptyPage();
        try {
            UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
            org.springframework.data.domain.Page<ThingEntity> pageResponse = repository.findByTenantIdAndTypeIgnoreCase(tenantId, type, pageable);

            return conversionService.convert(pageResponse, result.getClass());
        } catch (IllegalArgumentException e) {
            log.warn("Error processing URN: Tenant URN '{}'", tenantUrn);
        }
        return result;
    }

    // endregion

    // region Find By Type and URN

    @Override
    public Optional<ThingResponse> findByTypeAndUrn(String tenantUrn, String type, String urn) {

        try {
            UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
            UUID id = UuidUtil.getUuidFromUrn(urn);

            Optional<ThingEntity> entity = repository.findByIdAndTenantIdAndTypeIgnoreCase(id, tenantId, type);
            if (entity.isPresent()) {
                return Optional.ofNullable(conversionService.convert(entity.get(), ThingResponse.class));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Error processing URNs: Tenant URN '{}' - Thing URN '{}'", tenantUrn, urn);
        }

        return Optional.empty();
    }

    // endregion

    // region Find by Type and URN startsWith

    @Override
    public Page<ThingResponse> findByTypeAndUrnStartsWith(String tenantUrn, String type, String urnStartsWith) {
        throw new UnsupportedOperationException("The database implementation does not support 'startsWith' search for URNs");
    }

    @Override
    public Page<ThingResponse> findByTypeAndUrnStartsWith(String tenantUrn, String type, String urnStartsWith, Integer page, Integer number) {
        throw new UnsupportedOperationException("The database implementation does not support 'startsWith' search for URNs");
    }

    @Override
    public Page<ThingResponse> findByTypeAndUrnStartsWith(String tenantUrn, String type, String urnStartsWith, SortOrder sortOrder, String sortBy) {
        throw new UnsupportedOperationException("The database implementation does not support 'startsWith' search for URNs");
    }

    @Override
    public Page<ThingResponse> findByTypeAndUrnStartsWith(String tenantUrn, String type, String urnStartsWith, Integer page, Integer size,
                                                          SortOrder sortOrder, String sortBy) {
        throw new UnsupportedOperationException("The database implementation does not support 'startsWith' search for URNs");
    }

    private Page<ThingResponse> findByTypeAndUrnStartsWith(String tenantUrn, String type, String urnStartsWith, Pageable pageable) {
        throw new UnsupportedOperationException("The database implementation does not support 'startsWith' search for URNs");
    }

    // endregion

    // region Find by URNs

    @Override
    public List<ThingResponse> findByTypeAndUrns(String tenantUrn, String type, Collection<String> urns) {

        return findByTypeAndUrns(tenantUrn, type, urns, null);
    }

    @Override
    public List<ThingResponse> findByTypeAndUrns(String tenantUrn, String type, Collection<String> urns, SortOrder sortOrder, String sortBy) {

        sortBy = ThingPersistenceUtil.getSortByFieldName(sortBy);
        Sort.Direction direction = ThingPersistenceUtil.getSortDirection(sortOrder);
        Sort sort = new Sort(direction, sortBy);

        return findByTypeAndUrns(tenantUrn, type, urns, sort);
    }

    private List<ThingResponse> findByTypeAndUrns(String tenantUrn, String type, Collection<String> urns, Sort sort) {

        UUID tenantId;
        try {
            tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
        }
        catch (IllegalArgumentException e) {
            log.warn("Error processing URN: Tenant URN '{}'", tenantUrn);
            return new ArrayList<>();
        }

        List<UUID> ids = getUuidListFromUrnCollection(tenantUrn, urns);

        List<ThingEntity> entityList;
        if (sort != null) {
            entityList = repository.findByTenantIdAndTypeIgnoreCaseAndIdIn(tenantId, type, ids, sort);
        } else {
            entityList = repository.findByTenantIdAndTypeIgnoreCaseAndIdIn(tenantId, type, ids);
        }

        return convertList(entityList);
    }

    // endregion

    // region Find All

    @Override
    public Page<ThingResponse> findAll(String tenantUrn) {

        return findAll(tenantUrn, getPageable(null, null, null, null));
    }

    @Override
    public Page<ThingResponse> findAll(String tenantUrn, SortOrder sortOrder, String sortBy) {

        return findAll(tenantUrn, getPageable(null, null, sortBy, ThingPersistenceUtil.getSortDirection(sortOrder)));
    }

    @Override
    public Page<ThingResponse> findAll(String tenantUrn, Integer page, Integer size) {

        return findAll(tenantUrn, getPageable(page, size, null, null));
    }

    @Override
    public Page<ThingResponse> findAll(String tenantUrn, Integer page, Integer size, SortOrder sortOrder, String sortBy) {

       return findAll(tenantUrn, getPageable(page, size, sortBy, ThingPersistenceUtil.getSortDirection(sortOrder)));
    }

    private Page<ThingResponse> findAll(String tenantUrn, Pageable pageable) {

        Page<ThingResponse> result = ThingPersistenceUtil.emptyPage();
        try {
            UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
            org.springframework.data.domain.Page<ThingEntity> pageResponse = repository.findByTenantId(tenantId, pageable);

            return conversionService.convert(pageResponse, result.getClass());
        } catch (IllegalArgumentException e) {
            log.warn("Error processing URN: Tenant URN '{}'", tenantUrn);
        }

        return result;
    }

    /**
     * This is a temporary function for development purposes -- eventually we don't want
     * to support a "get everything" call, since theoretically that'd be billions of
     * objects.
     *
     * @return All the objects.
     */
    public Page<ThingResponse> getThings() {

        return convertPage(repository.findAll(getPageable(null, null, null, null)));
    }

    // endregion

    // region Helper Methods

    /**
     * Saves an object entity in an {@link ThingRepository}.
     *
     * @param objectEntity the object entity to persist
     * @return the persisted object entity
     * @throws ConstraintViolationException if the transaction fails due to violated constraints
     * @throws TransactionException         if the transaction fails because of something else
     */
    private ThingEntity persist(ThingEntity objectEntity) throws ConstraintViolationException, TransactionException {

        try {
            return repository.save(objectEntity);
        } catch (TransactionException e) {
            // we expect constraint violations to be the root cause for exceptions here,
            // so we throw this particular exception back to the caller
            if (ExceptionUtils.getRootCause(e) instanceof ConstraintViolationException) {
                throw (ConstraintViolationException) ExceptionUtils.getRootCause(e);
            } else {
                throw e;
            }
        }
    }

    private List<UUID> getUuidListFromUrnCollection(String tenantUrn, Collection<String> urns) {
        return urns.stream()
            .map(urn -> {
                try {
                    return UuidUtil.getUuidFromUrn(urn);
                } catch (IllegalArgumentException e) {
                    log.warn("Error processing URNs: Tenant URN '{}' - Thing URN '{}'", tenantUrn, urn);
                }
                return null;
            })
            .filter(uuid -> uuid != null)
            .collect(Collectors.toList());
    }

    private boolean alreadyExists(String tenantUrn, ThingCreate createThing) {

        return StringUtils.isNotBlank(createThing.getUrn()) && findByUrnAndType(tenantUrn, createThing.getUrn(), createThing.getType()).isPresent();
    }

    private Optional<ThingResponse> findByUrnAndType(String tenantUrn, String urn, String type) {

        try {
            UUID tenantId = UuidUtil.getUuidFromUrn(tenantUrn);
            UUID id = UuidUtil.getUuidFromUrn(urn);

            Optional<ThingEntity> entity = repository.findByIdAndTenantIdAndTypeIgnoreCase(id, tenantId, type);
            if (entity.isPresent()) {
                return Optional.ofNullable(conversionService.convert(entity.get(), ThingResponse.class));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Error processing URNs: Tenant URN '{}' - Thing URN '{}'", tenantUrn, urn);
        }

        return Optional.empty();
    }

    /**
     * Converts a single thing entity optional to the corresponding response object.
     *
     * @param entity the thing entity
     * @return an {@link Optional} that contains a {@link ThingResponse} instance or is empty
     */
    private Optional<ThingResponse> convertList(Optional<ThingEntity> entity) {

        if (entity.isPresent()) {
            final ThingResponse response = conversionService.convert(entity.get(), ThingResponse.class);
            return Optional.ofNullable(response);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Converts a list of thing entities to a list of corresponding response objects.
     *
     * @param entityPage the entities
     * @return the list of {@link ThingResponse} instances
     */
    private Page<ThingResponse> convertPage(org.springframework.data.domain.Page<ThingEntity> entityPage) {
        List<ThingResponse> responseData = entityPage.getContent().stream()
            .map(o -> conversionService.convert(o, ThingResponse.class))
            .collect(Collectors.toList());

        return Page.builder()
            .data((List) responseData)
            .page(PageInformation.builder()
                      .number(entityPage.getNumber())
                      .size(entityPage.getSize())
                      .totalElements(entityPage.getTotalElements())
                      .totalPages(entityPage.getTotalPages())
                      .build())
            .build();
    }

    /**
     * Converts a list of thing entities to a list of corresponding response objects.
     *
     * @param entityList the entities
     * @return the list of {@link ThingResponse} instances
     */
    private List<ThingResponse> convertList(List<ThingEntity> entityList) {
        return entityList.stream()
            .map(o -> conversionService.convert(o, ThingResponse.class))
            .collect(Collectors.toList());
    }


    /**
     * Builds the pageable for repository calls, including translation of 1-based page numbering on the API level to
     * 0-based page numbering on the repository level.
     *
     * @param page the page number
     * @param size the page size
     * @param sortBy the name of the field to sort by
     * @param direction the sort order direction
     * @return the pageable object
     */
    protected Pageable getPageable(Integer page, Integer size, String sortBy, Sort.Direction direction) {

        if (page == null) { page = DEFAULT_PAGE; }
        if (size == null) { size = DEFAULT_SIZE; }
        if (sortBy == null) { sortBy = DEFAULT_SORT_BY; }
        if (direction == null) { direction = DEFAULT_SORT_ORDER; }

        if ( page < 1) {
            throw new IllegalArgumentException("Page index must not be less than one!");
        }
        page = page - 1;

        if (StringUtils.isBlank(sortBy) || direction == null) {
            return new PageRequest(page, size);
        }

        return new PageRequest(page, size, direction, sortBy);
    }

    // endregion
}
