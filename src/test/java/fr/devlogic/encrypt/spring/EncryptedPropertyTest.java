package fr.devlogic.encrypt.spring;


import fr.devlogic.encrypt.spring.impl.EncryptedPropertyConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = {EncryptedPropertyConfiguration.class, DomainConfiguration.class, LdapProperties.class})
class EncryptedPropertyTest {

    @Autowired
    private DomainConfiguration domainConfiguration;

    @Autowired
    private LdapProperties ldapProperties;

    @Test
    void testDomain() {
        Assertions.assertThat(domainConfiguration.getPassword()).isEqualTo(Constantes.MOT_DE_PASSE);
        Assertions.assertThat(domainConfiguration.getUnencryptedPassword()).isEqualTo("123456");
        Assertions.assertThat(domainConfiguration.getUnencryptedPassword1()).isEqualTo("123456");
        Assertions.assertThat(domainConfiguration.getPassword2()).isEqualTo(Constantes.MOT_DE_PASSE);
        Assertions.assertThat(domainConfiguration.getPassword3()).isEqualTo(Constantes.MOT_DE_PASSE);
    }

    @Test
    void testLdap() {
        Assertions.assertThat(ldapProperties).isNotNull();
        Assertions.assertThat(ldapProperties.getPassword()).isEqualTo("secret");
    }
}
