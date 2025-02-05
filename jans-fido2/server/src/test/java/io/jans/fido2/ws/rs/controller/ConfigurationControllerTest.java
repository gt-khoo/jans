package io.jans.fido2.ws.rs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.fido2.model.error.ErrorResponseFactory;
import io.jans.fido2.service.DataMapperService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private ConfigurationController configurationController;

    @Mock
    private AppConfiguration appConfiguration;

    @Mock
    private DataMapperService dataMapperService;

    @Mock
    private ErrorResponseFactory errorResponseFactory;

    @Test
    void getConfiguration_ifFidoConfigurationIsNull_forbiddenException() {
        when(appConfiguration.getFido2Configuration()).thenReturn(null);
        when(errorResponseFactory.forbiddenException()).thenReturn(new WebApplicationException(Response.status(500).entity("test exception").build()));

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> configurationController.getConfiguration());
        assertNotNull(ex);
        assertNotNull(ex.getResponse());
        assertEquals(ex.getResponse().getStatus(), 500);
        assertEquals(ex.getResponse().getEntity(), "test exception");

        verify(appConfiguration).getFido2Configuration();
        verifyNoInteractions(dataMapperService);
        verifyNoMoreInteractions(appConfiguration);
    }

    @Test
    void getConfiguration_ifFido2ConfigurationNotNullAndSuperGluuEnabledIsTrue_success() throws JsonProcessingException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(true);
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode(), mapper.createObjectNode(), mapper.createObjectNode());
        String issuer = "https://jans-test.org";
        String baseEndpoint = issuer + "/fido";
        when(appConfiguration.getIssuer()).thenReturn(issuer);
        when(appConfiguration.getBaseEndpoint()).thenReturn(baseEndpoint);

        Response response = configurationController.getConfiguration();
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        JsonNode nodeEntity = assertJsonNode(response, issuer, baseEndpoint);

        assertTrue(nodeEntity.has("super_gluu_registration_endpoint"));
        assertEquals(nodeEntity.get("super_gluu_registration_endpoint").asText(), baseEndpoint + "/attestation/registration");
        assertTrue(nodeEntity.has("super_gluu_authentication_endpoint"));
        assertEquals(nodeEntity.get("super_gluu_authentication_endpoint").asText(), baseEndpoint + "/assertion/authentication");

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verify(dataMapperService, times(3)).createObjectNode();
    }

    @Test
    void getConfiguration_ifFido2ConfigurationNotNullAndSuperGluuEnabledIsFalse_success() throws JsonProcessingException {
        when(appConfiguration.getFido2Configuration()).thenReturn(mock(Fido2Configuration.class));
        when(appConfiguration.isSuperGluuEnabled()).thenReturn(false);
        when(dataMapperService.createObjectNode()).thenReturn(mapper.createObjectNode(), mapper.createObjectNode(), mapper.createObjectNode());
        String issuer = "https://jans-test.org";
        String baseEndpoint = issuer + "/fido";
        when(appConfiguration.getIssuer()).thenReturn(issuer);
        when(appConfiguration.getBaseEndpoint()).thenReturn(baseEndpoint);

        Response response = configurationController.getConfiguration();
        assertNotNull(response);
        assertEquals(response.getStatus(), 200);

        JsonNode nodeEntity = assertJsonNode(response, issuer, baseEndpoint);

        assertFalse(nodeEntity.has("super_gluu_registration_endpoint"));
        assertFalse(nodeEntity.has("super_gluu_authentication_endpoint"));

        verify(appConfiguration).getFido2Configuration();
        verify(appConfiguration).isSuperGluuEnabled();
        verify(dataMapperService, times(3)).createObjectNode();
    }

    @NotNull
    private JsonNode assertJsonNode(Response response, String issuer, String baseEndpoint) throws JsonProcessingException {
        JsonNode nodeEntity = mapper.readTree(response.getEntity().toString());
        assertTrue(nodeEntity.has("version"));
        assertEquals(nodeEntity.get("version").asText(), "1.1");
        assertTrue(nodeEntity.has("issuer"));
        assertEquals(nodeEntity.get("issuer").asText(), issuer);

        assertTrue(nodeEntity.has("attestation"));
        JsonNode attestationNode = nodeEntity.get("attestation");
        assertTrue(attestationNode.has("base_path"));
        assertEquals(attestationNode.get("base_path").asText(), baseEndpoint + "/attestation");
        assertTrue(attestationNode.has("options_endpoint"));
        assertEquals(attestationNode.get("options_endpoint").asText(), baseEndpoint + "/attestation/options");
        assertTrue(attestationNode.has("result_endpoint"));
        assertEquals(attestationNode.get("result_endpoint").asText(), baseEndpoint + "/attestation/result");

        assertTrue(nodeEntity.has("assertion"));
        JsonNode assertionNode = nodeEntity.get("assertion");
        assertTrue(assertionNode.has("base_path"));
        assertEquals(assertionNode.get("base_path").asText(), baseEndpoint + "/assertion");
        assertTrue(assertionNode.has("options_endpoint"));
        assertEquals(assertionNode.get("options_endpoint").asText(), baseEndpoint + "/assertion/options");
        assertTrue(assertionNode.has("result_endpoint"));
        assertEquals(assertionNode.get("result_endpoint").asText(), baseEndpoint + "/assertion/result");
        return nodeEntity;
    }
}
