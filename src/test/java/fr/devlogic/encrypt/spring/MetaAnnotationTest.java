package fr.devlogic.encrypt.spring;

import fr.devlogic.encrypt.spring.impl.EncryptedPropertyConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

@ActiveProfiles("dev")
@SpringBootTest(classes = EncryptedPropertyConfiguration.class)
class MetaAnnotationTest {
    @Test
    void findMetaAnnotation() {
        AnnotatedTypeScanner annotatedTypeScanner = new AnnotatedTypeScanner(EncryptedProperty.class);
        Set<Class<?>> types = annotatedTypeScanner.findTypes("fr.");
        System.out.println(types);
        Assertions.assertTrue(true);
    }
}
