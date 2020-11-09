package fr.devlogic.encrypt.spring;


import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.function.BiConsumer;

@ConfigurationProperties("domain")
@Configuration
@EnableConfigurationProperties
public class DomainConfiguration {
    private String user;
    private String password;
    private String password2;
    private String password3;
    private String unencryptedPassword;
    private String unencryptedPassword1;

    @SuppressWarnings("unused")
    @EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY, profiles = "dev")
    private static final BiConsumer<LdapProperties, String> LDAPPROPERTIES_SETPASSWORD = LdapProperties::setPassword;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    @EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY, profiles = "dev")
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    @ProprieteChiffree(profiles = "*")
    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getUnencryptedPassword() {
        return unencryptedPassword;
    }

    public String getPassword3() {
        return password3;
    }

    @ProprieteChiffree
    public void setPassword3(String password3) {
        this.password3 = password3;
    }

    @EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY, profiles = "!dev")
    public void setUnencryptedPassword(String unencryptedPassword) {
        this.unencryptedPassword = unencryptedPassword;
    }

    public String getUnencryptedPassword1() {
        return unencryptedPassword1;
    }

    @ProprieteChiffree(profiles = "!*")
    public void setUnencryptedPassword1(String unencryptedPassword1) {
        this.unencryptedPassword1 = unencryptedPassword1;
    }
}
