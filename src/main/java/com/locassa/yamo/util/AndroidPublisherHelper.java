package com.locassa.yamo.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class AndroidPublisherHelper {

    static final String MIME_TYPE_APK = "application/vnd.android.package-archive";
    private static final Log log = LogFactory.getLog(AndroidPublisherHelper.class);
    /**
     * Path to the private key file (only used for Service Account auth).
     */
    private static final String SRC_RESOURCES_KEY_P12 = "https://s3-eu-west-1.amazonaws.com/yamo-resources/certificates/key.p12";

    /**
     * Path to the client secrets file (only used for Installed Application
     * auth).
     */
    private static final String RESOURCES_CLIENT_SECRETS_JSON = "/resources/google/client_secrets.json";

    /**
     * Directory to store user credentials (only for Installed Application
     * auth).
     */
    private static final String DATA_STORE_SYSTEM_PROPERTY = "user.home";
    private static final String DATA_STORE_FILE = ".store/android_publisher_api";
    private static final File DATA_STORE_DIR =
            new File(System.getProperty(DATA_STORE_SYSTEM_PROPERTY), DATA_STORE_FILE);

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /**
     * Installed application user ID.
     */
    private static final String INST_APP_USER_ID = "user";
    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;
    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to
     * make it a single globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    private static Credential authorizeWithServiceAccount(String serviceAccountEmail)
            throws GeneralSecurityException, IOException, URISyntaxException {
        log.info(String.format("Authorizing using Service Account: %s", serviceAccountEmail));

        // Build service account credential.

        File pk12File = new File(String.format("%s" + File.separator + "android_key.p12", YamoUtils.TMP_DIR));
        FileUtils.copyURLToFile(new URL(SRC_RESOURCES_KEY_P12), pk12File);

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId(serviceAccountEmail)
                .setServiceAccountScopes(
                        Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER))
                .setServiceAccountPrivateKeyFromP12File(pk12File)
                .build();
        return credential;
    }

    /**
     * Authorizes the installed application to access user's protected data.
     *
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private static Credential authorizeWithInstalledApplication() throws IOException {
//        log.info("Authorizing using installed application");
//
//        // load client secrets
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
//                JSON_FACTORY,
//                new InputStreamReader(
//                        AndroidPublisherHelper.class
//                                .getResourceAsStream(RESOURCES_CLIENT_SECRETS_JSON)));
//        // Ensure file has been filled out.
//        checkClientSecretsFile(clientSecrets);
//
//        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
//
//        // set up authorization code flow
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
//                .Builder(HTTP_TRANSPORT,
//                JSON_FACTORY, clientSecrets,
//                Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER))
//                .setDataStoreFactory(dataStoreFactory).build();
//        // authorize
//        return new AuthorizationCodeInstalledApp(
//                flow, new LocalServerReceiver()).authorize(INST_APP_USER_ID);
        return null;
    }

    /**
     * Ensure the client secrets file has been filled out.
     *
     * @param clientSecrets the GoogleClientSecrets containing data from the
     *                      file
     */
    private static void checkClientSecretsFile(GoogleClientSecrets clientSecrets) {
        if (clientSecrets.getDetails().getClientId().startsWith("[[INSERT")
                || clientSecrets.getDetails().getClientSecret().startsWith("[[INSERT")) {
            log.error("Enter Client ID and Secret from "
                    + "APIs console into resources/client_secrets.json.");
            System.exit(1);
        }
    }

    /**
     * Performs all necessary setup steps for running requests against the API
     * using the Installed Application auth method.
     *
     * @param applicationName the name of the application: com.example.app
     * @return the {@Link AndroidPublisher} service
     */
    protected static AndroidPublisher init(String applicationName) throws Exception {
        return init(applicationName, null);
    }

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @param applicationName     the name of the application: com.example.app
     * @param serviceAccountEmail the Service Account Email (empty if using
     *                            installed application)
     * @return the {@Link AndroidPublisher} service
     * @throws GeneralSecurityException
     * @throws IOException
     */
    protected static AndroidPublisher init(String applicationName,
                                           @Nullable String serviceAccountEmail) throws IOException, GeneralSecurityException, URISyntaxException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(applicationName),
                "applicationName cannot be null or empty!");

        // Authorization.
        newTrustedTransport();
        Credential credential;
        if (serviceAccountEmail == null || serviceAccountEmail.isEmpty()) {
            credential = authorizeWithInstalledApplication();
        } else {
            credential = authorizeWithServiceAccount(serviceAccountEmail);
        }

        // Set up and return API client.
        return new AndroidPublisher.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(applicationName)
                .build();
    }

    private static void newTrustedTransport() throws GeneralSecurityException,
            IOException {
        if (null == HTTP_TRANSPORT) {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        }
    }

}
