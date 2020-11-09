# Configuration properties encryption library

This library enables encryption of properties in Spring configuration classes.

## Example of use

In the application.yml configuration file, the `password` property is encrypted:

```yaml
domain:
  user: username
  password: "cXRmmH67QY9DxbHrlSrDhw=="
```

The configuration is mapped into the DomainConfiguration class:

```java
@ConfigurationProperties("domain")
@Configuration
@EnableConfigurationProperties
public class DomainConfiguration {
    private String user;
    private String password;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    @EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY, profiles="!test")
    public void setPassword(String password) {
        this.password = password;
    }
}
```

A property, of type String, is declared encrypted by adding the `@EncryptedProperty` annotation. The algorithm and the key (binary stored in base 64) are indicated
on the setter of the property (the getter of the property must be declared). you can factorize the key and the algorithm by putting the annotation on the class, and thus not set them about the methods.

Example of key and algo:
* algo : AES
* clef : 4QJ9YpTDKkrEEaJcbhn6DU6SgaSW+cNWC66CW6unmPc=

The size of the key indicates the strength of the encryption. For AES, the size is 128, 192 or 256 bits. In the previous example, the key size is 256 bits.

Property encryption can be conditioned on enabled profiles. In the `profiles` attribute is listed all accepted or rejected profiles. A rejected profile is prefixed with `!`. The profiles are separated by commas, you can put spaces (example: "prod, pre-prod, !test"). You can use the wildcard `"*"` (and its opposite `"!*"`). The rule is: the property is not encrypted when one of the rejected profiles is activated, or when one of the activated profiles is not present. In the "prod, pre-prod, !test" example, the encryption will take place for the prod or pre-prod profiles, and not for the test profile.
### Property encryption meta-annotation

Configurations of the `@EncryptedProperty` annotation can be factorized using a meta-annotation. To do this, simply define an annotation annotated with `@EncryptedProperty` where parameters (at least the key and the algo) are specified. For example :

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EncryptedProperty(algo = "AES", key = "4QJ9YpTDKkrEEaJcbhn6DU6SgaSW+cNWC66CW6unmPc=", profiles = "!dev, !test")
public @interface ProprieteChiffree {
}
```

And the use of the annotation:

```java
    @ProprieteChiffree
    public void setPassword(String password) {
        this.password = password;
    }
```

You can add a profile attributes to the meta-annotation to override the profiles for certain properties:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EncryptedProperty(algo = "AES", key = "4QJ9YpTDKkrEEaJcbhn6DU6SgaSW+cNWC66CW6unmPc=", profiles = "!dev, !test")
public @interface ProprieteChiffree {
    String profiles() default "";
}
```

### Third-party property encryption

### Encryption of third-party configurations

In some situations, you want to be able to enforce property encryption on third-party configurations. For example, for LDAP:

```yaml
spring:
  profiles: ldpap-local
  ldap:
    urls: ldaps://localhost:636
    base: valeur-base
    username: valeur-username
    password: valeur-mot-de-passe
```

The password must be encrypted, it corresponds to the `LdapProperties.setPassword(String password)` method. Encryption is declared in a configuration bean:

```java
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    // ...
    
    @SuppressWarnings("unused")
    @EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY, profiles = "dev")
    private static BiConsumer<LdapProperties, String> LDAPPROPERTIES_SETPASSWORD = LdapProperties::setPassword;

    @SuppressWarnings("unused")
    @EncryptedProperty(algo = Constantes.ALGO_CRYPTO, key = Constantes.CRYPTO_KEY, profiles = "dev")
    private static BiConsumer<Credential, String> CREDENTIAL_SETPASSWORD = Credential::setPassword;
    
    // ...
}
```

All you have to do is declare a static field, which can be private, of the `BiConsumer` type. The field must imperatively be initialized by the lambda which points to the setter of the field to be decrypted. Therefore, the first parameter of the `BiConsumer` is the class of the targeted configuration, the second parameter is always `String`. The name of the field is arbitrary, the field is annotated in the same way as the "set" method (by the `@EncryptedProperty` annotation or a meta-annotation).

## Property encryption

The library allows to generate a key or the encryption of the properties.

* Launching the jar:
```shell script
$ java -jar  chiffrement-proprietes-spring.jar
```

Produces : 
```text
usage:
        -genKey sizeInBits
        -crypt algo key data1 [data2...]
```

* To generate a key, just specify the size of the key:
* 
```shell script
$ java -jar  chiffrement-proprietes-spring.jar -genKey 256
```

Produces the key: 

```text
MqK9yaoMD6DtpshTjPiCWWj7wUrOwrZDAQSoxUTp9kI=
```

* Property encryption: 

```shell script
$ java -jar  chiffrement-proprietes-spring.jar -crypt AES "MqK9yaoMD6DtpshTjPiCWWj7wUrOwrZDAQSoxUTp9kI=" 'prop1' 'prop2' 'prop3'
```

Produces :

```text
prop1 -> "gHYoTBvvOab9lQBxEBQzmg=="
prop2 -> "rKPW9laAK3mrBM7+zkLioA=="
prop3 -> "fiA0e2U7+MDf+EYfUdT3MQ=="
```

## How it is working

A spring post bean processes all beans that are initialized. This bean only targets beans with the `@Configuration` or `@ConfigurationProperties` annotation. It looks for methods with the `@EncryptedProperty` annotation, or meta-annotations, beginning with "set". For these methods, the getter is executed, then the search for the algo and the key is done preferably on the method and by default on the class. If the key or algo is missing, an `EncryptedPropertyBeanProcessorException` exception is thrown. The property is decrypted and injected by the setter call.
