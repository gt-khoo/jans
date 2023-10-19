package io.jans.keycloak.link.server.service;

import io.jans.keycloak.link.model.config.CacheRefreshConfiguration;
import io.jans.keycloak.link.model.config.KeycloakConfiguration;
import io.jans.keycloak.link.service.config.ConfigurationFactory;
import io.jans.link.util.PropertyUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class KeycloakService implements Serializable{

    @Inject
    private Logger log;
    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private PropertyUtil propertyUtil;

    boolean isEncoded;

    public Keycloak getKeycloakInstance() {
        CacheRefreshConfiguration cacheRefreshConfiguration = configurationFactory.getAppConfiguration();

        KeycloakConfiguration keycloakConfiguration = cacheRefreshConfiguration.getKeycloakConfiguration();
        String clientSecret = propertyUtil.decryptString(keycloakConfiguration.getClientSecret());
        if(clientSecret == null){
            log.info("clientSecret is null or not encrypted");
            clientSecret = keycloakConfiguration.getClientSecret();
        }

        String password = propertyUtil.decryptString(keycloakConfiguration.getPassword());
        if (password == null){
            log.info("password is null or not encrypted");
            password = keycloakConfiguration.getPassword();
        }

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        Keycloak instance = KeycloakBuilder.builder()
                .serverUrl(keycloakConfiguration.getServerUrl())
                .realm(keycloakConfiguration.getRealm())
                .clientId(keycloakConfiguration.getClientId())
                .clientSecret(clientSecret)
                .grantType(keycloakConfiguration.getGrantType())
                .username(keycloakConfiguration.getUsername())
                .password(password)
                .build();
        log.info("getKeycloakInstance() instance::"+instance.getClass());

        return instance;
    }

    @Produces
    @ApplicationScoped
    public List<UserRepresentation> getUserList() {
        Keycloak keycloak = getKeycloakInstance();
        List<UserRepresentation> finalUsersResourceList = new ArrayList<>();

        RealmsResource RealmsResource = keycloak.realms();
        List<RealmRepresentation> RealmResource = RealmsResource.findAll();

        for(RealmRepresentation realmRepresentation : RealmResource){
            log.info("check " + realmRepresentation.getRealm());
            String realm = realmRepresentation.getRealm();
            List<UserRepresentation> usersResourceList = keycloak.realm(realm).users().list();
            finalUsersResourceList.addAll(usersResourceList);
        }

        for(UserRepresentation userRepresentation : finalUsersResourceList){
            log.info("username  " + userRepresentation.getUsername());
        }

        return finalUsersResourceList;
    }

    public Keycloak getKeycloakInstanceDecrypt() {
        CacheRefreshConfiguration cacheRefreshConfiguration = configurationFactory.getAppConfiguration();

        KeycloakConfiguration  keycloakConfiguration = cacheRefreshConfiguration.getKeycloakConfiguration();

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        Keycloak instance = KeycloakBuilder.builder()
                .serverUrl(keycloakConfiguration.getServerUrl())
                .realm(keycloakConfiguration.getRealm())
                .clientId(keycloakConfiguration.getClientId())
                .clientSecret(keycloakConfiguration.getClientSecret())
                //.grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .grantType(keycloakConfiguration.getGrantType())
                .username(keycloakConfiguration.getUsername())
                .password(keycloakConfiguration.getPassword())
                //.resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
                .build();
        log.info("getKeycloakInstance() instance::"+instance.getClass());

        return instance;
    }
}
