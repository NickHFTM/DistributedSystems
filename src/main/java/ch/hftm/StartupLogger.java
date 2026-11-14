package ch.hftm;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class StartupLogger {

    void onStart(@Observes StartupEvent event) {
        Log.info("Blog Backend wurde erfolgreich gestartet");
        Log.info("Persistenz ist konfiguriert");
        Log.info("Authentifizierung: OIDC / Keycloak konfiguriert");
    }
}