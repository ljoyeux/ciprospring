package fr.devlogic.encrypt.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In a spring configuration class {@link org.springframework.context.annotation.Configuration} or
 * {@link org.springframework.boot.context.properties.ConfigurationProperties}, the EncryptedProperty annotation
 * added on the setter indicates that the property is encrypted.
 * <p>
 * The annotation is imperatively present on the setter, the getter must also be defined. Exemple :
 * <pre>
 *  &#64;EncryptedProperty(algo = "AES", key = "Lx2slKtvTQUcnavFCiPLWo306waGuRqt+Y2ues/6rkA=", profiles = "!dev, !test")
 *  public void setPassword(String password) {
 *      this.password = password;
 *  }
 * </pre>
 * <p>
 * Annotation attributes can be factorized by annotating the configuration class and not specifying the attributes
 * on setters (if not to override them).
 * <p>
 * A meta-annotation can be used to factorize attributes. The specified annotation is annotated by this
 * annotation. Retention is {@link RetentionPolicy#RUNTIME} and target is {@link ElementType#METHOD} Exemple :
 * <pre>
 *  &#64;Retention(RetentionPolicy.RUNTIME)
 *  &#64;Target(ElementType.METHOD)
 *  &#64;EncryptedProperty(algo = "AES", key = "Lx2slKtvTQUcnavFCiPLWo306waGuRqt+Y2ues/6rkA=", profiles = "!dev, !test")
 *  public &#64;interface EncryptedPassword {
 *  }
 * </pre>
 *<p>
 * Third parties proprieties can be encrypted by annotating a static field with {@link java.util.function.BiConsumer} type in a
 * configuration bean. The first paramater of the BiConsumer is the targeted type and the second paramater is {@link String} en second type.
 * The field is initialised by a function pointer. For example :
 * <pre>
 *     &#64;SuppressWarnings("unused")
 *     &#64;EncryptedProperty(algo = "AES", key = "Lx2slKtvTQUcnavFCiPLWo306waGuRqt+Y2ues/6rkA=, profiles = "!dev")
 *     private static final BiConsumer&#60;LdapProperties, String&#62; LDAPPROPERTIES_SETPASSWORD = LdapProperties::setPassword;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.FIELD})
public @interface EncryptedProperty {
    /**
     * cipher algorithm
     *
     * @return
     * algorithm  (e.g. "AES")
     */
    String algo() default "";

    /**
     * Base 64 cipher key
     *
     * @return
     * Base 64 cipher key
     */
    String key() default "";

    /**
     * List of accepted or rejected spring profiles
     *
     * A profile prefixed with "!" is rejected. By default, all profiles are affected.
     *
     * @return
     * Profiles List, separated by commas (with or without spaces), for example "prod, !test".
     */
    String profiles() default "";
}
