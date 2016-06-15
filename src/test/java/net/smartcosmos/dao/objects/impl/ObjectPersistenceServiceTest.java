package net.smartcosmos.dao.objects.impl;

import net.smartcosmos.dao.objects.ObjectDao.QueryParameterType;
import net.smartcosmos.dao.things.ThingPersistenceConfig;
import net.smartcosmos.dao.objects.ObjectPersistenceTestApplication;
import net.smartcosmos.dao.things.domain.ThingEntity;
import net.smartcosmos.dao.things.impl.ThingPersistenceService;
import net.smartcosmos.dao.things.repository.ThingRepository;
import net.smartcosmos.dto.objects.ObjectCreate;
import net.smartcosmos.dto.objects.ObjectResponse;
import net.smartcosmos.dto.objects.ObjectUpdate;
import net.smartcosmos.security.user.SmartCosmosUser;
import net.smartcosmos.util.UuidUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.smartcosmos.dao.objects.ObjectDao.QueryParameterType.EXACT;
import static net.smartcosmos.dao.objects.ObjectDao.QueryParameterType.MODIFIED_AFTER;
import static net.smartcosmos.dao.objects.ObjectDao.QueryParameterType.MONIKER_LIKE;
import static net.smartcosmos.dao.objects.ObjectDao.QueryParameterType.NAME_LIKE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { ObjectPersistenceTestApplication.class,
                                            ThingPersistenceConfig.class })
@ActiveProfiles("test")
@WebAppConfiguration
@IntegrationTest({ "spring.cloud.config.enabled=false", "eureka.client.enabled:false" })
public class ObjectPersistenceServiceTest {

    public static final int DELAY_BETWEEN_LAST_MODIFIED_DATES = 10;
    public static final String OBJECT_URN_QUERY_PARAMS_01 = "objectUrnQueryParams01";
    public static final String OBJECT_URN_QUERY_PARAMS_02 = "objectUrnQueryParams02";
    public static final String OBJECT_URN_QUERY_PARAMS_03 = "objectUrnQueryParams03";
    public static final String OBJECT_URN_QUERY_PARAMS_04 = "objectUrnQueryParams04";
    public static final String OBJECT_URN_QUERY_PARAMS_05 = "objectUrnQueryParams05";
    public static final String OBJECT_URN_QUERY_PARAMS_06 = "objectUrnQueryParams06";
    public static final String OBJECT_URN_QUERY_PARAMS_07 = "objectUrnQueryParams07";
    public static final String OBJECT_URN_QUERY_PARAMS_08 = "objectUrnQueryParams08";
    public static final String OBJECT_URN_QUERY_PARAMS_09 = "objectUrnQueryParams09";
    public static final String OBJECT_URN_QUERY_PARAMS_10 = "objectUrnQueryParams10";
    public static final String OBJECT_URN_QUERY_PARAMS_11 = "objectUrnQueryParams11";
    public static final String OBJECT_URN_QUERY_PARAMS_12 = "objectUrnQueryParams12";
    public static final String NAME_ONE = "name one";
    public static final String TYPE_ONE = "type one";
    public static final String NAME_TWO = "name two";
    public static final String NAME_THREE = "name three";
    public static final String TYPE_TWO = "type two";
    public static final String WHATEVER = "whatever";
    public static final String MONIKER_ONE = "moniker one";
    public static final String MONIKER_TWO = "moniker two";
    public static final String MONIKER_THREE = "moniker three";
    public static final String OBJECT_URN_QUERY_PARAMS = "objectUrnQueryParams";
    public static final String OBJECT_URN_QUERY_PARAMS_0 = "objectUrnQueryParams0";
    public static final String OBJECT_URN_QUERY_PARAMS_1 = "objectUrnQueryParams1";
    public static final String OBJECT_URN_QUERY_PARAMS_99 = "objectUrnQueryParams99";
    public static final String BJECT_URN_QUERY_PARAMS = "bjectUrnQueryParams";
    private final UUID accountId = UUID.randomUUID();

    private final String accountUrn = UuidUtil.getAccountUrnFromUuid(accountId);

    @Autowired
    ThingPersistenceService objectPersistenceService;

    @Autowired
    ThingRepository objectRepository;

    @Before
    public void setUp() throws Exception {

        // Need to mock out user for conversion service.
        // Might be a good candidate for a test package util.
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal())
            .thenReturn(new SmartCosmosUser(accountUrn, "urn:userUrn", "username",
                                            "password", Arrays.asList(new SimpleGrantedAuthority("USER"))));
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @After
    public void tearDown() throws Exception {
        objectRepository.deleteAll();
    }

    @Test
    public void create() throws Exception {
        ObjectCreate create = ObjectCreate.builder().objectUrn("urn:fakeUrn")
            .moniker("moniker").description("description").name("name").type("type")
            .build();
        ObjectResponse response = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, "urn:fakeUrn");

        assertTrue(entity.isPresent());

        assertEquals("urn:fakeUrn", entity.get().getUrn());
        assertEquals("urn:fakeUrn", response.getObjectUrn());
    }

    // region Update

    @Test
    public void thatUpdateByObjectUrnSucceeds() {

        String objectUrn = "urn:fakeUrn-update";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newName = "new name";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .objectUrn(objectUrn)
            .name(newName)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(newName, responseUpdate.get().getName());
        assertEquals(type, responseUpdate.get().getType());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(moniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateByUrnSucceeds() {

        String objectUrn = "urn:fakeUrn-update2";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newName = "new name";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .name(newName)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(newName, responseUpdate.get().getName());
        assertEquals(type, responseUpdate.get().getType());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(moniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateTypeSucceeds() {

        String objectUrn = "urn:fakeUrn-update7";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newType = "new type";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .type(newType)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(name, responseUpdate.get().getName());
        assertEquals(newType, responseUpdate.get().getType());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(moniker, responseUpdate.get().getMoniker());
    }

    @Test(expected=ConstraintViolationException.class)
    public void thatUpdateEmptyTypeFails() {

        String objectUrn = "urn:fakeUrn-update12";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newType = "";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .type(newType)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(name, responseUpdate.get().getName());
        assertEquals(newType, responseUpdate.get().getType());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(moniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateDescriptionSucceeds() {

        String objectUrn = "urn:fakeUrn-update8";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newDescription = "new description";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .description(newDescription)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(name, responseUpdate.get().getName());
        assertEquals(type, responseUpdate.get().getType());
        assertEquals(newDescription, responseUpdate.get().getDescription());
        assertEquals(moniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateEmptyDescriptionSucceeds() {

        String objectUrn = "urn:fakeUrn-update9";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newDescription = "";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .description(newDescription)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(name, responseUpdate.get().getName());
        assertEquals(type, responseUpdate.get().getType());
        assertEquals(newDescription, responseUpdate.get().getDescription());
        assertEquals(moniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateMonikerSucceeds() {

        String objectUrn = "urn:fakeUrn-update10";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newMoniker = "new moniker";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .moniker(newMoniker)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(name, responseUpdate.get().getName());
        assertEquals(type, responseUpdate.get().getType());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(newMoniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateEmptyMonikerSucceeds() {

        String objectUrn = "urn:fakeUrn-update11";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newMoniker = "";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .moniker(newMoniker)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertTrue(responseUpdate.isPresent());

        assertEquals(objectUrn, responseUpdate.get().getObjectUrn());
        assertEquals(responseCreate.getUrn(), responseUpdate.get().getUrn());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(type, responseUpdate.get().getType());
        assertEquals(description, responseUpdate.get().getDescription());
        assertEquals(newMoniker, responseUpdate.get().getMoniker());
    }

    @Test
    public void thatUpdateNonexistentFails() {
        ObjectUpdate update = ObjectUpdate.builder()
            .objectUrn("urn:DOES-NOT-EXIST")
            .name("new name")
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);

        assertFalse(responseUpdate.isPresent());
    }

    @Test(expected=IllegalArgumentException.class)
    public void thatUpdateWithoutIdThrowsException() {

        String objectUrn = "urn:fakeUrn-update3";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newName = "new name";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .name(newName)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);
    }

    @Test(expected=IllegalArgumentException.class)
    public void thatUpdateByOverspecifiedIdThrowsException() {

        String objectUrn = "urn:fakeUrn-update4";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newName = "new name";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .objectUrn(objectUrn)
            .name(newName)
            .build();
        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);
    }

    @Test(expected=IllegalArgumentException.class)
    public void thatUpdateByOverspecifiedAndConflictingIdThrowsException() {

        String objectUrn = "urn:fakeUrn-update5";
        String name = "name";
        String type = "type";
        String description = "description";
        String moniker = "moniker";

        String newName = "new name";

        ObjectCreate create = ObjectCreate.builder()
            .objectUrn(objectUrn)
            .name(name)
            .type(type)
            .description(description)
            .moniker(moniker)
            .build();

        ObjectResponse responseCreate = objectPersistenceService
            .create("urn:account:URN-IN-AUDIT-TRAIL", create);

        Optional<ThingEntity> entity = objectRepository
            .findByAccountIdAndObjectUrn(accountId, objectUrn);

        assertTrue(entity.isPresent());

        assertEquals(objectUrn, entity.get().getUrn());
        assertEquals(objectUrn, responseCreate.getObjectUrn());

        ObjectUpdate update = ObjectUpdate.builder()
            .urn(responseCreate.getUrn())
            .objectUrn("urn:fakeUrn-update-different")
            .name(newName)
            .build();

        Optional<ObjectResponse> responseUpdate = objectPersistenceService.update(accountUrn, update);
    }

    // endregion

    // region Find By Object URN

    @Test
    public void testFindByObjectUrn() throws Exception {

        final UUID accountUuid = UUID.randomUUID();
        final String accountUrn = UuidUtil.getAccountUrnFromUuid(accountUuid);

        ThingEntity entity = ThingEntity.builder().accountId(accountUuid)
            .objectUrn("urn").name("my object name").type("some type").build();

        this.objectRepository.save(entity);

        Optional<ObjectResponse> response = objectPersistenceService
            .findByObjectUrn(accountUrn, "urn");

        assertTrue(response.isPresent());
    }

    @Test
    public void testFindByObjectUrnStartsWithNonexistent() throws Exception {
        populateQueryData();

        List<ObjectResponse> response = objectPersistenceService.findByObjectUrnStartsWith(accountUrn, "no-such-urn");

        assertTrue(response.isEmpty());
    }

    @Test
    public void testFindByObjectUrnStartsWith() throws Exception {
        populateQueryData();

        List<ObjectResponse> response = objectPersistenceService.findByObjectUrnStartsWith(accountUrn, OBJECT_URN_QUERY_PARAMS);

        assertEquals(12, response.size());
    }

    // endregion

    // region Find by Query Parameters

    // no query data should return an empty response
    @Test
    public void testFindByQueryParameters_NoQueryParameters() throws Exception {

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        expectedSize = 0;
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // should also be OK to pass in a null as parameter map
        expectedSize = 0;
        response = objectPersistenceService.findByQueryParameters(accountUrn, null);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);
    }

    @Test
    public void testFindByQueryParameters_ObjectUrnLike() throws Exception {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        expectedSize = 12;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS);
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 9;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_0);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 3;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_1);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 1;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_11);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_99);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, BJECT_URN_QUERY_PARAMS);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 3;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS);
        queryParams.put(QueryParameterType.TYPE, "type one");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    @Test
    public void testFindByQueryParameters_ObjectUrnLike_Exact() throws Exception
    {
        populateQueryData();
        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        // baseline - no exact flag
        expectedSize = 9;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_0);
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // exact = false, which should be a noop
        expectedSize = 9;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_0);
        queryParams.put(QueryParameterType.EXACT, false);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // exact = true should return no elements, since there's no exact match
        expectedSize = 0;
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS_0);
        queryParams.put(QueryParameterType.EXACT, true);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    @Test
    public void testFindByQueryParameters_Type() throws Exception {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        expectedSize = 3;
        queryParams.put(QueryParameterType.TYPE, "type one");
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);


        // verify that matching of the type field is exact
        expectedSize = 0;
        queryParams.put(QueryParameterType.TYPE, "type o");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.TYPE, "ype one");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.TYPE, "type z");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    @Test
    public void testFindByQueryParameters_NameLike() throws Exception {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        expectedSize = 9;
        queryParams.remove(QueryParameterType.TYPE);
        queryParams.put(NAME_LIKE, "name");
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 5;
        queryParams.put(QueryParameterType.NAME_LIKE, "name o");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 2;
        queryParams.put(QueryParameterType.NAME_LIKE, "name three");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.NAME_LIKE, "name z");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.NAME_LIKE, "ame");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);
    }

    @Test
    public void testFindByQueryParameters_NameLike_Exact() throws Exception
    {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        // baseline - no exact flag
        expectedSize = 9;
        queryParams.remove(QueryParameterType.TYPE);
        queryParams.put(NAME_LIKE, "name");
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // exact = false, which should be a noop
        expectedSize = 9;
        queryParams.remove(QueryParameterType.TYPE);
        queryParams.put(NAME_LIKE, "name");
        queryParams.put(QueryParameterType.EXACT, false);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // exact = true should return no elements, since there's no exact match
        expectedSize = 0;
        queryParams.remove(QueryParameterType.TYPE);
        queryParams.put(NAME_LIKE, "name");
        queryParams.put(QueryParameterType.EXACT, true);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    @Test
    public void testFindByQueryParameters_MonikerLike() throws Exception {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        expectedSize = 3;
        queryParams.remove(QueryParameterType.NAME_LIKE);
        queryParams.put(MONIKER_LIKE, "moniker");
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 1;
        queryParams.put(QueryParameterType.MONIKER_LIKE, "moniker o");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 1;
        queryParams.put(QueryParameterType.MONIKER_LIKE, "moniker three");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.MONIKER_LIKE, "moniker z");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(QueryParameterType.MONIKER_LIKE, "oniker");
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);
    }

    @Test
    public void testFindByQueryParameters_MonikerLike_Exact() throws Exception
    {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        // baseline - no exact flag
        expectedSize = 3;
        queryParams.remove(QueryParameterType.NAME_LIKE);
        queryParams.put(MONIKER_LIKE, "moniker");
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // exact = false, which should be a noop
        expectedSize = 3;
        queryParams.remove(QueryParameterType.NAME_LIKE);
        queryParams.put(MONIKER_LIKE, "moniker");
        queryParams.put(QueryParameterType.EXACT, false);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        // exact = true should return no elements, since there's no exact match
        expectedSize = 0;
        queryParams.remove(QueryParameterType.NAME_LIKE);
        queryParams.put(MONIKER_LIKE, "moniker");
        queryParams.put(QueryParameterType.EXACT, true);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);



    }

        @Test
    public void testFindByQueryParameters_LastModified() throws Exception {

        final UUID accountUuid = UuidUtil.getNewUuid();
        final String accountUrn = UuidUtil.getAccountUrnFromUuid(accountUuid);

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        int expectedSize = 0;
        int actualSize = 0;

        Long firstDate = new Date().getTime();
        Thread.sleep(DELAY_BETWEEN_LAST_MODIFIED_DATES);

        ThingEntity firstObject = ThingEntity.builder().accountId(accountUuid)
            .objectUrn("objectUrnLastModTest1").name("last mod test 1").type("anythingIsFine").build();
        this.objectRepository.save(firstObject);

        Long secondDate = new Date().getTime();
        Thread.sleep(DELAY_BETWEEN_LAST_MODIFIED_DATES);

        ThingEntity secondObject = ThingEntity.builder().accountId(accountUuid)
            .objectUrn("objectUrnLastModTest2").name("last mod test 2").type("anythingIsFine").build();
        this.objectRepository.save(secondObject);

        Long thirdDate = new Date().getTime();
        Thread.sleep(DELAY_BETWEEN_LAST_MODIFIED_DATES);

        ThingEntity thirdObject = ThingEntity.builder().accountId(accountUuid)
            .objectUrn("objectUrnLastModTest3").name("last mod test 3").type("anythingIsFine").build();
        this.objectRepository.save(thirdObject);

        Long fourthDate = new Date().getTime();

        expectedSize = 3;
        queryParams.put(MODIFIED_AFTER, firstDate);
        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 2;
        queryParams.put(MODIFIED_AFTER, secondDate);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 1;
        queryParams.put(MODIFIED_AFTER, thirdDate);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

        expectedSize = 0;
        queryParams.put(MODIFIED_AFTER, fourthDate);
        response = objectPersistenceService.findByQueryParameters(accountUrn, queryParams);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);
    }

    /**
     * Test case for OBJECTS-725 findByQueryParams ignores account
     * @throws Exception
     */
    @Test
    public void testFindByQueryParametersDifferentAccountUrns() throws Exception {
        populateQueryData();

        Map<QueryParameterType, Object> queryParams = new HashMap<>();
        queryParams.put(QueryParameterType.OBJECT_URN_LIKE, OBJECT_URN_QUERY_PARAMS);

        final UUID newAccountUuid = UuidUtil.getNewUuid();
        final String newAccountUrn = UuidUtil.getAccountUrnFromUuid(newAccountUuid);

        int expectedSize = 0;
        int actualSize = 0;

        List<ObjectResponse> response = objectPersistenceService.findByQueryParameters(newAccountUrn, queryParams);
        actualSize = response.size();

        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);
    }

    @Test
    public void testFindByUrns() throws Exception
    {
        populateQueryData();

        int expectedSize = 0;
        int actualSize = 0;

        String firstUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_01).get().getUrn();
        String secondUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_02).get().getUrn();
        String thirdUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_03).get().getUrn();

        Collection<String> urns = new ArrayList<>();
        urns.add(firstUrn);
        urns.add(secondUrn);
        urns.add(thirdUrn);

        expectedSize = 3;
        List<Optional<ObjectResponse>> response = objectPersistenceService.findByUrns(accountUrn, urns);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    @Test
    public void thatFindByUrnsReturnsPartialResultsWithNonexistentUrn() throws Exception
    {
        populateQueryData();

        int expectedSize = 0;
        int actualSize = 0;

        String firstUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_01).get().getUrn();
        String secondUrn = UuidUtil.getUrnFromUuid(UuidUtil.getNewUuid());
        String thirdUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_03).get().getUrn();

        Collection<String> urns = new ArrayList<>();
        urns.add(firstUrn);
        urns.add(secondUrn);
        urns.add(thirdUrn);

        expectedSize = 3;
        List<Optional<ObjectResponse>> response = objectPersistenceService.findByUrns(accountUrn, urns);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    @Test
    public void thatFindByUrnsReturnsPartialResultsWithUnparseableUrn() throws Exception
    {
        populateQueryData();

        int expectedSize = 0;
        int actualSize = 0;

        String firstUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_01).get().getUrn();
        String secondUrn = "cannot be parsed as URN";
        String thirdUrn = objectPersistenceService.findByObjectUrn(accountUrn, OBJECT_URN_QUERY_PARAMS_03).get().getUrn();

        Collection<String> urns = new ArrayList<>();
        urns.add(firstUrn);
        urns.add(secondUrn);
        urns.add(thirdUrn);

        expectedSize = 3;
        List<Optional<ObjectResponse>> response = objectPersistenceService.findByUrns(accountUrn, urns);
        actualSize = response.size();
        assertTrue("Expected " + expectedSize + " but received " + actualSize, actualSize == expectedSize);

    }

    // endregion

    // region Helper Methods

    // used by findByQueryParametersStringParameters()
    private void populateQueryData() throws Exception {

        ThingEntity entityNameOneTypeOne = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_01).name(NAME_ONE).type(TYPE_ONE).build();

        ThingEntity entityNameTwoTypeOne = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_02).name(NAME_TWO).type(TYPE_ONE).build();

        ThingEntity entityNameThreeTypeOne = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_03).name(NAME_THREE).type(TYPE_ONE).build();

        ThingEntity entityNameOneTypeTwo = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_04).name(NAME_ONE).type(TYPE_TWO).build();

        ThingEntity entityNameTwoTypeTwo = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_05).name(NAME_TWO).type(TYPE_TWO).build();

        ThingEntity entityNameThreeTypeTwo = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_06).name(NAME_THREE).type(TYPE_TWO).build();

        ThingEntity entityNameOneMonikerOne = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_07).name(NAME_ONE).type(WHATEVER).moniker(MONIKER_ONE).build();

        ThingEntity entityNameOneMonikerTwo = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_08).name(NAME_ONE).type(WHATEVER).moniker(MONIKER_TWO).build();

        ThingEntity entityNameOneMonikerThree = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_09).name(NAME_ONE).type(WHATEVER).moniker(MONIKER_THREE).build();

        ThingEntity entityObjectUrn10 = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_10).name(WHATEVER).type(WHATEVER).build();

        ThingEntity entityObjectUrn11 = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_11).name(WHATEVER).type(WHATEVER).build();

        ThingEntity entityObjectUrn12 = ThingEntity.builder().accountId(accountId)
            .objectUrn(OBJECT_URN_QUERY_PARAMS_12).name(WHATEVER).type(WHATEVER).build();

        objectRepository.save(entityNameOneTypeOne);
        objectRepository.save(entityNameTwoTypeOne);
        objectRepository.save(entityNameThreeTypeOne);
        objectRepository.save(entityNameOneTypeTwo);
        objectRepository.save(entityNameTwoTypeTwo);
        objectRepository.save(entityNameThreeTypeTwo);
        objectRepository.save(entityNameOneMonikerOne);
        objectRepository.save(entityNameOneMonikerTwo);
        objectRepository.save(entityNameOneMonikerThree);
        objectRepository.save(entityObjectUrn10);
        objectRepository.save(entityObjectUrn11);
        objectRepository.save(entityObjectUrn12);
    }

    // endregion

}
